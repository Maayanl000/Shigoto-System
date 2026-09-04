package com.shigoto.backend.service;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.dto.HrApplicationSummaryDTO;
import com.shigoto.backend.dto.HrApplicationDetailsDTO;
import com.shigoto.backend.dto.StaffApplicationResponseDTO;
import com.shigoto.backend.dto.InterviewerSubmittedTaskDTO;
import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus; // הנה ה-import הנקי שהוספנו!
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.TaskReviewDecision;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.entity.GithubData;
import com.shigoto.backend.entity.GithubAnalysisStatus;
import com.shigoto.backend.exception.DuplicateApplicationException;
import com.shigoto.backend.exception.ApplicationDeleteConflictException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.repository.GithubDataRepository;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.messaging.NotificationEventPublisher;
import com.shigoto.backend.messaging.GithubAnalysisEventPublisher;
import com.shigoto.backend.messaging.GithubAnalysisRequestedEvent;
import com.shigoto.backend.util.GithubProfileUrlParser;
import com.shigoto.backend.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.io.Resource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CvStorageService cvStorageService;
    private final NotificationEventPublisher notificationEventPublisher;
    private final InterviewRepository interviewRepository;
    private final GithubAnalysisEventPublisher githubAnalysisEventPublisher;
    private final GithubDataRepository githubDataRepository;

    @Transactional
    public ApplicationResponseDTO createApplication(User candidate, Long jobId, String coverLetter, MultipartFile cv) {
        if (candidate.getRole() != Role.CANDIDATE) {
            throw new IllegalArgumentException("Referenced user is not a candidate");
        }

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalArgumentException("Job is not open for applications");
        }

        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getId(), jobId)) {
            throw new DuplicateApplicationException("Candidate has already applied for this job");
        }

        String storageKey = cvStorageService.store(cv);
        try {
            Application application = Application.builder()
                    .candidate(candidate)
                    .job(job)
                    .cvUrl(storageKey)
                    .coverLetter(coverLetter)
                    .build();
            Application saved = applicationRepository.saveAndFlush(application);
            publish(saved, NotificationType.APPLICATION_SUBMITTED);
            requestGithubAnalysis(saved);
            return toResponseDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            deleteStoredCvAfterFailedApplication(storageKey);
            if (applicationRepository.existsByCandidateIdAndJobId(candidate.getId(), jobId)) {
                throw new DuplicateApplicationException("Candidate has already applied for this job");
            }
            throw ex;
        } catch (RuntimeException ex) {
            deleteStoredCvAfterFailedApplication(storageKey);
            throw ex;
        }
    }
    // פונקציה למחיקת מועמדות לפי ID
    @Transactional
    public void deleteApplication(Long applicationId, Long expectedVersion, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        requireExpectedVersion(application, expectedVersion);
        if (interviewRepository.existsByApplicationId(applicationId)) {
            throw new ApplicationDeleteConflictException(
                    "Application cannot be deleted because interview history exists");
        }
        applicationRepository.delete(application);
        applicationRepository.flush();
        cvStorageService.delete(application.getCvUrl());
    }
    // הוסיפי את הפונקציה הזו בתוך ApplicationService

    @Transactional(readOnly = true)
    public List<HrApplicationSummaryDTO> getAllApplications(User hr) {
        return getAllApplications(hr, null);
    }

    @Transactional(readOnly = true)
    public List<HrApplicationSummaryDTO> getAllApplications(User hr, Long jobId) {
        requireHrWithCompany(hr);
        List<Application> applications = jobId == null
                ? applicationRepository.findByJobCompany(hr.getCompany())
                : applicationRepository.findByJobIdAndJobCompany(jobId, hr.getCompany());
        return applications
                .stream()
                .map(application -> HrApplicationSummaryDTO.from(application,
                        interviewRepository.findFirstByApplicationIdAndStatusOrderByScheduledAtDesc(
                                        application.getId(), com.shigoto.backend.entity.InterviewStatus.SCHEDULED)
                                .map(com.shigoto.backend.entity.Interview::getType)
                                .orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public HrApplicationDetailsDTO getHrApplicationDetails(Long applicationId, User hr) {
        return HrApplicationDetailsDTO.from(findHrCompanyApplication(applicationId, hr));
    }

    public CvDownload getHrApplicationCv(Long applicationId, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        return new CvDownload("cv-application-" + applicationId + ".pdf",
                cvStorageService.load(application.getCvUrl()));
    }

    @Transactional
    public HrApplicationDetailsDTO updateHrNotes(
            Long applicationId, String hrNotes, Long expectedVersion, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        requireExpectedVersion(application, expectedVersion);
        String normalizedNotes = hrNotes == null || hrNotes.isBlank() ? null : hrNotes.trim();
        if (normalizedNotes != null && normalizedNotes.length() > 10_000) {
            throw new IllegalArgumentException("HR notes must not exceed 10000 characters");
        }
        application.setHrNotes(normalizedNotes);
        return HrApplicationDetailsDTO.from(applicationRepository.saveAndFlush(application));
    }

    @Transactional
    public HrApplicationDetailsDTO transitionHrApplicationStatus(
            Long applicationId, ApplicationStatus targetStatus, Long expectedVersion, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        requireExpectedVersion(application, expectedVersion);
        ApplicationStatus currentStatus = application.getStatus();
        if (!isAllowedHrTransition(currentStatus, targetStatus)) {
            throw new IllegalArgumentException(
                    "Application cannot move from " + currentStatus + " to " + targetStatus);
        }
        if (targetStatus == ApplicationStatus.OFFER
                && !interviewRepository.existsByApplicationIdAndTypeAndStatus(
                applicationId, com.shigoto.backend.entity.InterviewType.TECHNICAL,
                com.shigoto.backend.entity.InterviewStatus.COMPLETED)) {
            throw new IllegalArgumentException(
                    "Offer requires a completed technical interview");
        }
        if (currentStatus == ApplicationStatus.HR_INTERVIEW && targetStatus == ApplicationStatus.APPLIED) {
            if (application.getTaskInstructions() != null || application.getTaskDeadline() != null
                    || application.getTaskRepoUrl() != null || application.getTaskReviewNotes() != null) {
                throw new IllegalArgumentException(
                        "Application cannot return to APPLIED while home task data exists");
            }
            if (interviewRepository.existsByApplicationIdAndTypeAndStatusNot(
                    application.getId(), com.shigoto.backend.entity.InterviewType.HR,
                    com.shigoto.backend.entity.InterviewStatus.CANCELED)) {
                throw new IllegalArgumentException(
                        "Application cannot return to APPLIED while an HR interview is scheduled or completed");
            }
        }
        application.transitionTo(targetStatus);
        HrApplicationDetailsDTO result = HrApplicationDetailsDTO.from(applicationRepository.saveAndFlush(application));
        if (targetStatus == ApplicationStatus.REJECTED) {
            publish(application, NotificationType.APPLICATION_REJECTED);
        } else if (targetStatus == ApplicationStatus.OFFER) {
            publish(application, NotificationType.APPLICATION_OFFERED);
        } else if (targetStatus == ApplicationStatus.HIRED) {
            publish(application, NotificationType.APPLICATION_HIRED);
        }
        return result;
    }

    @Transactional
    public HrApplicationDetailsDTO rejectHrApplication(
            Long applicationId, String candidateFeedback, Long expectedVersion, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        requireExpectedVersion(application, expectedVersion);
        if (!isAllowedHrTransition(application.getStatus(), ApplicationStatus.REJECTED)) {
            throw new IllegalArgumentException(
                    "Application cannot move from " + application.getStatus() + " to REJECTED");
        }
        application.setCandidateFeedback(normalizeCandidateFeedback(candidateFeedback));
        application.transitionTo(ApplicationStatus.REJECTED);
        HrApplicationDetailsDTO result = HrApplicationDetailsDTO.from(applicationRepository.saveAndFlush(application));
        publish(application, NotificationType.APPLICATION_REJECTED);
        return result;
    }

    @Transactional
    public HrApplicationDetailsDTO updateCandidateFeedback(
            Long applicationId, String candidateFeedback, Long expectedVersion, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        requireExpectedVersion(application, expectedVersion);
        if (application.getStatus() != ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("Candidate feedback can be edited only for a rejected application");
        }
        application.setCandidateFeedback(normalizeCandidateFeedback(candidateFeedback));
        return HrApplicationDetailsDTO.from(applicationRepository.saveAndFlush(application));
    }

    @Transactional
    public HrApplicationDetailsDTO assignHomeTask(
            Long applicationId, String taskInstructions, LocalDateTime deadline, Long reviewerId,
            Long expectedVersion, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        requireExpectedVersion(application, expectedVersion);
        if (application.getStatus() != ApplicationStatus.HR_INTERVIEW) {
            throw new IllegalArgumentException("Home task can only be sent after the HR interview stage");
        }
        String normalizedInstructions = taskInstructions == null ? null : taskInstructions.trim();
        if (normalizedInstructions == null || normalizedInstructions.isEmpty()) {
            throw new IllegalArgumentException("Home task instructions are required");
        }
        if (normalizedInstructions.length() > 10_000) {
            throw new IllegalArgumentException("Home task instructions must not exceed 10000 characters");
        }
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Home task deadline must be in the future");
        }
        if (reviewerId == null) {
            throw new IllegalArgumentException("Home task reviewer is required");
        }
        User reviewer = userRepository.findByIdAndCompany(reviewerId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found"));
        if (reviewer.getRole() != Role.INTERVIEWER) {
            throw new IllegalArgumentException("Selected reviewer is not an interviewer");
        }
        application.setTaskInstructions(normalizedInstructions);
        application.setTaskDeadline(deadline);
        application.setTaskRepoUrl(null);
        application.setTaskReviewNotes(null);
        application.setTaskReviewer(reviewer);
        application.transitionTo(ApplicationStatus.TASK_SENT);
        HrApplicationDetailsDTO result = HrApplicationDetailsDTO.from(applicationRepository.saveAndFlush(application));
        publish(application, NotificationType.HOME_TASK_ASSIGNED);
        return result;
    }

    @Transactional
    public HrApplicationDetailsDTO updateHomeTaskDeadline(
            Long applicationId, LocalDateTime deadline, Long expectedVersion, User hr) {
        Application application = findHrCompanyApplication(applicationId, hr);
        requireExpectedVersion(application, expectedVersion);
        if (application.getStatus() != ApplicationStatus.TASK_SENT) {
            throw new IllegalArgumentException("Home task deadline can be updated only before task submission");
        }
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Home task deadline must be in the future");
        }
        application.setTaskDeadline(deadline);
        HrApplicationDetailsDTO result = HrApplicationDetailsDTO.from(applicationRepository.saveAndFlush(application));
        publish(application, NotificationType.HOME_TASK_UPDATED);
        return result;
    }

    private boolean isAllowedHrTransition(ApplicationStatus currentStatus, ApplicationStatus targetStatus) {
        if (targetStatus == ApplicationStatus.REJECTED) {
            return currentStatus != ApplicationStatus.OFFER
                    && currentStatus != ApplicationStatus.HIRED
                    && currentStatus != ApplicationStatus.REJECTED;
        }
        return switch (currentStatus) {
            case APPLIED -> targetStatus == ApplicationStatus.HR_INTERVIEW;
            case HR_INTERVIEW -> targetStatus == ApplicationStatus.APPLIED;
            case TECH_INTERVIEW_SCHEDULED -> targetStatus == ApplicationStatus.OFFER;
            case OFFER -> targetStatus == ApplicationStatus.HIRED;
            default -> false;
        };
    }

    @Transactional(readOnly = true)
    public List<InterviewerSubmittedTaskDTO> getSubmittedTasksForInterviewer(User interviewer) {
        requireInterviewerWithCompany(interviewer);
        return applicationRepository.findByStatusAndTaskReviewerIdAndJobCompanyOrderByAppliedAtAsc(
                        ApplicationStatus.TASK_SUBMITTED, interviewer.getId(), interviewer.getCompany())
                .stream().map(InterviewerSubmittedTaskDTO::from).toList();
    }

    @Transactional
    public InterviewerSubmittedTaskDTO reviewSubmittedTask(
            Long applicationId, TaskReviewDecision decision, Long expectedVersion, User interviewer) {
        requireInterviewerWithCompany(interviewer);
        if (decision == null) {
            throw new IllegalArgumentException("Task review decision is required");
        }
        Application application = applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(
                        applicationId, interviewer.getCompany(), interviewer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        requireExpectedVersion(application, expectedVersion);
        if (application.getStatus() != ApplicationStatus.TASK_SUBMITTED) {
            throw new IllegalArgumentException("Only a submitted task can be reviewed");
        }
        boolean activeTechnicalInterview = decision == TaskReviewDecision.APPROVE
                && interviewRepository.existsByApplicationIdAndTypeAndStatus(
                applicationId, com.shigoto.backend.entity.InterviewType.TECHNICAL,
                com.shigoto.backend.entity.InterviewStatus.SCHEDULED);
        application.transitionTo(decision == TaskReviewDecision.REJECT
                ? ApplicationStatus.REJECTED
                : activeTechnicalInterview
                        ? ApplicationStatus.TECH_INTERVIEW_SCHEDULED
                        : ApplicationStatus.TASK_APPROVED);
        InterviewerSubmittedTaskDTO result = InterviewerSubmittedTaskDTO.from(applicationRepository.saveAndFlush(application));
        if (decision == TaskReviewDecision.REJECT) publish(application, NotificationType.APPLICATION_REJECTED);
        return result;
    }

    @Transactional
    public InterviewerSubmittedTaskDTO updateTaskReviewNotes(
            Long applicationId, String notes, Long expectedVersion, User interviewer) {
        requireInterviewerWithCompany(interviewer);
        Application application = applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(
                        applicationId, interviewer.getCompany(), interviewer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        requireExpectedVersion(application, expectedVersion);
        if (application.getStatus() != ApplicationStatus.TASK_SUBMITTED) {
            throw new IllegalArgumentException("Private task notes are available only while a task awaits review");
        }
        String normalized = notes == null ? "" : notes.trim();
        if (normalized.length() > 10000) {
            throw new IllegalArgumentException("Private task notes must be at most 10000 characters");
        }
        application.setTaskReviewNotes(normalized.isEmpty() ? null : normalized);
        return InterviewerSubmittedTaskDTO.from(applicationRepository.saveAndFlush(application));
    }

    @Transactional(readOnly = true)
    public List<StaffApplicationResponseDTO> getApplicationsByCandidate(Long candidateId, User hr) {
        requireHrWithCompany(hr);
        var candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new IllegalArgumentException("Referenced user is not a candidate");
        }

        return applicationRepository.findByCandidateIdAndJobCompanyOrderByAppliedAtDesc(
                        candidateId, hr.getCompany())
                .stream()
                .map(StaffApplicationResponseDTO::from)
                .toList();
    }

    public List<ApplicationResponseDTO> getApplicationsForCandidate(User candidate) {
        requireCandidateRole(candidate);
        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidate.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ApplicationResponseDTO getOwnedApplicationById(Long applicationId, User candidate) {
        return toResponseDTO(findOwnedApplication(applicationId, candidate));
    }

    public CvDownload getOwnedCv(Long applicationId, User candidate) {
        Application application = findOwnedApplication(applicationId, candidate);
        Resource resource = cvStorageService.load(application.getCvUrl());
        return new CvDownload("cv-application-" + applicationId + ".pdf", resource);
    }

    public ApplicationResponseDTO submitTask(
            Long applicationId, String repositoryUrl, Long expectedVersion, User candidate) {
        Application application = findOwnedApplication(applicationId, candidate);
        requireExpectedVersion(application, expectedVersion);
        if (application.getStatus() == ApplicationStatus.TASK_SUBMITTED) {
            throw new IllegalArgumentException("Technical task has already been submitted");
        }
        if (application.getStatus() != ApplicationStatus.TASK_SENT) {
            throw new IllegalArgumentException("Application status does not allow technical task submission");
        }
        if (application.getTaskDeadline() == null) {
            throw new IllegalArgumentException("Technical task has no assigned deadline");
        }

        LocalDateTime serverTime = LocalDateTime.now();
        if (!application.getTaskDeadline().isAfter(serverTime)) {
            throw new IllegalArgumentException("Technical task deadline has passed");
        }

        String normalizedRepositoryUrl = validateRepositoryUrl(repositoryUrl);
        application.setTaskRepoUrl(normalizedRepositoryUrl);
        application.transitionTo(ApplicationStatus.TASK_SUBMITTED);

        return toResponseDTO(applicationRepository.saveAndFlush(application));
    }

    private Application findOwnedApplication(Long applicationId, User candidate) {
        requireCandidateRole(candidate);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));
        if (!Objects.equals(application.getCandidate().getId(), candidate.getId())) {
            throw new AccessDeniedException("Application does not belong to the authenticated candidate");
        }
        return application;
    }

    private Application findHrCompanyApplication(Long applicationId, User hr) {
        requireHrWithCompany(hr);
        return applicationRepository.findByIdAndJobCompany(applicationId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private void requireCandidateRole(User candidate) {
        if (candidate == null || candidate.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Candidate access is required");
        }
    }

    private void requireHrWithCompany(User hr) {
        if (hr == null || hr.getRole() != Role.HR) {
            throw new AccessDeniedException("HR access is required");
        }
        if (hr.getCompany() == null) {
            throw new AccessDeniedException("HR user must belong to a company");
        }
    }

    private void requireInterviewerWithCompany(User interviewer) {
        if (interviewer == null || interviewer.getRole() != Role.INTERVIEWER) {
            throw new AccessDeniedException("Interviewer access is required");
        }
        if (interviewer.getCompany() == null) {
            throw new AccessDeniedException("Interviewer must belong to a company");
        }
    }

    private void deleteStoredCvAfterFailedApplication(String storageKey) {
        try {
            cvStorageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // Preserve the original database error; storage cleanup can be investigated separately.
        }
    }

    public record CvDownload(String downloadFilename, Resource resource) {}

    private String validateRepositoryUrl(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new IllegalArgumentException("Repository URL is required");
        }

        String trimmedUrl = repositoryUrl.trim();
        try {
            URI uri = new URI(trimmedUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            boolean validScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            boolean validHost = "github.com".equalsIgnoreCase(host)
                    || "www.github.com".equalsIgnoreCase(host);
            long pathSegments = Arrays.stream(uri.getPath().split("/"))
                    .filter(segment -> !segment.isBlank())
                    .count();

            if (!validScheme || !validHost || pathSegments < 2) {
                throw new IllegalArgumentException(
                        "Repository URL must be a valid GitHub repository URL in the form /owner/repository");
            }
            if (trimmedUrl.length() > 255) {
                throw new IllegalArgumentException("Repository URL must not exceed 255 characters");
            }
            return trimmedUrl;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Repository URL is malformed");
        }
    }

    private ApplicationResponseDTO toResponseDTO(Application application) {
        var job = application.getJob();
        return new ApplicationResponseDTO(
                application.getId(),
                application.getCandidate().getId(),
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation(),
                application.getCoverLetter(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getTaskDeadline(),
                application.getTaskInstructions(),
                application.getTaskRepoUrl(),
                application.getStatus() == ApplicationStatus.REJECTED
                        ? application.getCandidateFeedback() : null,
                application.getCvUrl() != null && !application.getCvUrl().isBlank(),
                application.getVersion()
        );
    }

    private void requireExpectedVersion(Application application, Long expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("Application version is required");
        }
        if (!expectedVersion.equals(application.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Application.class, application.getId());
        }
    }

    private String normalizeCandidateFeedback(String feedback) {
        String normalized = feedback == null || feedback.isBlank() ? null : feedback.trim();
        if (normalized != null && normalized.length() > 10000) {
            throw new IllegalArgumentException("Candidate feedback must not exceed 10000 characters");
        }
        return normalized;
    }

    private void publish(Application application, NotificationType type) {
        notificationEventPublisher.publishAfterCommit(CandidateNotificationEvent.of(type,
                application.getCandidate().getId(), application.getId(), null));
    }

    private void requestGithubAnalysis(Application application) {
        User candidate = application.getCandidate();
        GithubProfileUrlParser.extractUsername(candidate.getGithubProfileUrl()).ifPresent(username -> {
            GithubAnalysisRequestedEvent event = GithubAnalysisRequestedEvent.of(
                    candidate.getId(), application.getId(), username);
            GithubData data = candidate.getGithubData();
            if (data == null) {
                data = GithubData.builder().candidate(candidate).username(username)
                        .status(GithubAnalysisStatus.PENDING).lastEventId(event.eventId()).build();
                candidate.setGithubData(data);
                githubDataRepository.save(data);
            } else if (!data.getUsername().equalsIgnoreCase(username)
                    || data.getStatus() == GithubAnalysisStatus.FAILED) {
                data.setUsername(username);
                data.setStatus(GithubAnalysisStatus.PENDING);
                data.setPublicRepositoryCount(null);
                data.setTopLanguages(new java.util.ArrayList<>());
                data.setLatestPushAt(null);
                data.setAnalyzedAt(null);
                data.setLastEventId(event.eventId());
                githubDataRepository.save(data);
            }
            githubAnalysisEventPublisher.publishAfterCommit(event);
        });
    }
}
