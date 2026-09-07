package com.shigoto.backend.service;

import com.shigoto.backend.dto.CandidateInterviewResponseDTO;
import com.shigoto.backend.dto.HrInterviewerOptionDTO;
import com.shigoto.backend.dto.HrInterviewScheduleRequestDTO;
import com.shigoto.backend.dto.HrInterviewRescheduleRequestDTO;
import com.shigoto.backend.dto.HrScheduledInterviewResponseDTO;
import com.shigoto.backend.dto.InterviewerInterviewResponseDTO;
import com.shigoto.backend.dto.InterviewerCandidateReviewDTO;
import com.shigoto.backend.entity.*;
import com.shigoto.backend.exception.InterviewSlotConflictException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.messaging.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Manages company-scoped interview scheduling, candidate visibility, interviewer feedback, and workflow notifications.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    static final String ACTIVE_SLOT_INDEX = "uk_interviews_active_interviewer_slot";
    private static final String SLOT_CONFLICT_MESSAGE =
            "Interviewer is no longer available at the selected time";

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * Schedules an interview after validating company ownership, workflow stage, interviewer membership, timing, and slot availability.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param hr the authenticated HR user defining company scope
     * @return the persisted interview as shown to HR
     */
    @Transactional
    public HrScheduledInterviewResponseDTO scheduleInterview(
            Long applicationId, HrInterviewScheduleRequestDTO request, User hr) {
        // Validate company scope, required scheduling inputs, and future timing.
        requireHrWithCompany(hr);
        if (request == null || request.interviewerId() == null || request.type() == null
                || request.scheduledAt() == null) {
            throw new IllegalArgumentException("Interviewer, interview type, and scheduled time are required");
        }
        if (!request.scheduledAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Interview must be scheduled in the future");
        }
        String meetingLink = validateMeetingLink(request.meetingLink());

        // Lock the operation to the current application version and permitted workflow stage.
        Application application = applicationRepository.findByIdAndJobCompany(applicationId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        requireExpectedVersion(application, request.applicationVersion());
        if (application.getStatus() == ApplicationStatus.OFFER
                || application.getStatus() == ApplicationStatus.HIRED
                || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot schedule an interview for a terminal application");
        }
        validateInterviewStage(application, request.type());

        // Resolve a company interviewer and reject duplicate application or interviewer slots.
        User interviewer = userRepository.findByIdAndCompany(request.interviewerId(), hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found"));
        if (interviewer.getRole() != Role.INTERVIEWER) {
            throw new IllegalArgumentException("Selected user is not an interviewer");
        }
        if (interviewRepository.existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNot(
                applicationId, interviewer.getId(), request.scheduledAt(), InterviewStatus.CANCELED)) {
            throw new IllegalArgumentException("This interview is already scheduled");
        }
        if (interviewRepository.existsByInterviewerIdAndScheduledAtAndStatusNot(
                interviewer.getId(), request.scheduledAt(), InterviewStatus.CANCELED)) {
            throw new IllegalArgumentException("Interviewer already has an interview at this time");
        }

        // Persist the interview and related workflow transition before publishing the notification.
        Interview interview = Interview.builder()
                .application(application)
                .interviewer(interviewer)
                .scheduledAt(request.scheduledAt())
                .meetingLink(meetingLink)
                .type(request.type())
                .status(InterviewStatus.SCHEDULED)
                .build();
        if (request.type() == InterviewType.TECHNICAL) {
            application.transitionTo(ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        } else if (request.type() == InterviewType.HR) {
            application.transitionTo(ApplicationStatus.HR_INTERVIEW);
        }
        applicationRepository.save(application);
        Interview saved = saveWithSlotConflictMapping(interview);
        publish(saved, NotificationType.INTERVIEW_SCHEDULED);
        return HrScheduledInterviewResponseDTO.from(saved);
    }

    /**
     * Lists interviewer accounts belonging to the authenticated HR user's company.
     * @param hr the authenticated HR user defining company scope
     * @return selectable interviewer identities for the HR user's company
     */
    @Transactional(readOnly = true)
    public List<HrInterviewerOptionDTO> getCompanyInterviewers(User hr) {
        requireHrWithCompany(hr);
        return userRepository.findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(Role.INTERVIEWER, hr.getCompany())
                .stream().map(HrInterviewerOptionDTO::from).toList();
    }

    /**
     * Reschedules a company interview after checking its version, future time, replacement interviewer, and slot availability.
     * @param interviewId the interview identifier
     * @param request the request payload
     * @param hr the authenticated HR user defining company scope
     * @return the persisted interview with its revised schedule
     */
    @Transactional
    public HrScheduledInterviewResponseDTO rescheduleInterview(
            Long interviewId, HrInterviewRescheduleRequestDTO request, User hr) {
        // Revalidate company scope, future timing, and the interview's optimistic-lock version.
        requireHrWithCompany(hr);
        if (request == null || request.interviewerId() == null || request.scheduledAt() == null) {
            throw new IllegalArgumentException("Interviewer and scheduled time are required");
        }
        if (!request.scheduledAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Interview must be scheduled in the future");
        }
        String meetingLink = validateMeetingLink(request.meetingLink());
        Interview interview = findHrCompanyInterview(interviewId, hr);
        requireExpectedVersion(interview, request.version());
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only a scheduled interview can be rescheduled");
        }
        // Check the replacement interviewer against all other active slots before mutation.
        User interviewer = userRepository.findByIdAndCompany(request.interviewerId(), hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found"));
        if (interviewer.getRole() != Role.INTERVIEWER) {
            throw new IllegalArgumentException("Selected user is not an interviewer");
        }
        Long applicationId = interview.getApplication().getId();
        if (interviewRepository.existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNotAndIdNot(
                applicationId, interviewer.getId(), request.scheduledAt(), InterviewStatus.CANCELED, interviewId)) {
            throw new IllegalArgumentException("This interview is already scheduled");
        }
        if (interviewRepository.existsByInterviewerIdAndScheduledAtAndStatusNotAndIdNot(
                interviewer.getId(), request.scheduledAt(), InterviewStatus.CANCELED, interviewId)) {
            throw new IllegalArgumentException("Interviewer already has an interview at this time");
        }
        interview.setInterviewer(interviewer);
        interview.setScheduledAt(request.scheduledAt());
        interview.setMeetingLink(meetingLink);
        Interview saved = saveWithSlotConflictMapping(interview);
        publish(saved, NotificationType.INTERVIEW_RESCHEDULED);
        return HrScheduledInterviewResponseDTO.from(saved);
    }

    /**
     * Cancels a company interview after verifying that the supplied version is current.
     * @param interviewId the interview identifier
     * @param expectedVersion the client-visible version used for optimistic locking
     * @param hr the authenticated HR user defining company scope
     * @return the persisted interview in the cancelled state
     */
    @Transactional
    public HrScheduledInterviewResponseDTO cancelInterview(Long interviewId, Long expectedVersion, User hr) {
        Interview interview = findHrCompanyInterview(interviewId, hr);
        requireExpectedVersion(interview, expectedVersion);
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only a scheduled interview can be canceled");
        }
        interview.setStatus(InterviewStatus.CANCELED);
        Application application = interview.getApplication();
        if (interview.getType() == InterviewType.TECHNICAL
                && application.getStatus() == ApplicationStatus.TECH_INTERVIEW_SCHEDULED
                && !interviewRepository.existsByApplicationIdAndTypeAndStatusAndIdNot(
                        application.getId(), InterviewType.TECHNICAL, InterviewStatus.SCHEDULED, interviewId)) {
            application.transitionTo(hasApprovedHomeTask(application)
                    ? ApplicationStatus.TASK_APPROVED
                    : hasHomeTask(application)
                            ? ApplicationStatus.TASK_SENT
                            : ApplicationStatus.HR_INTERVIEW);
            applicationRepository.saveAndFlush(application);
        }
        Interview saved = interviewRepository.saveAndFlush(interview);
        publish(saved, NotificationType.INTERVIEW_CANCELED);
        return HrScheduledInterviewResponseDTO.from(saved);
    }

    /**
     * Lists interviews for an application owned by the authenticated HR user's company.
     * @param applicationId the application identifier
     * @param hr the authenticated HR user defining company scope
     * @return HR-facing interview details for the application
     */
    @Transactional(readOnly = true)
    public List<HrScheduledInterviewResponseDTO> getHrApplicationInterviews(Long applicationId, User hr) {
        requireHrWithCompany(hr);
        applicationRepository.findByIdAndJobCompany(applicationId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId)
                .stream().map(HrScheduledInterviewResponseDTO::from).toList();
    }

    /**
     * Lists interviews for an application after verifying that the application belongs to the candidate.
     * @param applicationId the application identifier
     * @param candidate the candidate being processed
     * @return candidate-visible interview details for the owned application
     */
    @Transactional(readOnly = true)
    public List<CandidateInterviewResponseDTO> getCandidateInterviews(Long applicationId, User candidate) {
        if (candidate == null || candidate.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Candidate access is required");
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));
        if (!Objects.equals(application.getCandidate().getId(), candidate.getId())) {
            throw new AccessDeniedException("Application does not belong to the authenticated candidate");
        }
        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId)
                .stream()
                .map(this::toCandidateResponseDTO)
                .toList();
    }

    /**
     * Lists all interviews associated with applications owned by the candidate.
     * @param candidate the candidate being processed
     * @return candidate-visible interview details across the candidate's applications
     */
    @Transactional(readOnly = true)
    public List<CandidateInterviewResponseDTO> getCandidateInterviews(User candidate) {
        requireCandidate(candidate);
        return interviewRepository.findByApplicationCandidateIdOrderByScheduledAtAsc(candidate.getId())
                .stream()
                .map(this::toCandidateResponseDTO)
                .toList();
    }

    /**
     * Lists interviews assigned to the authenticated interviewer.
     * @param interviewer the authenticated interviewer
     * @return interviewer-facing details for assigned interviews
     */
    @Transactional(readOnly = true)
    public List<InterviewerInterviewResponseDTO> getInterviewerInterviews(User interviewer) {
        requireInterviewer(interviewer);
        return interviewRepository.findByInterviewerIdOrderByScheduledAtAsc(interviewer.getId())
                .stream()
                .map(InterviewerInterviewResponseDTO::from)
                .toList();
    }

    /**
     * Completes an assigned interview with validated feedback after checking the interview version.
     * @param interviewId the interview identifier
     * @param feedback the feedback text
     * @param expectedVersion the client-visible version used for optimistic locking
     * @param interviewer the authenticated interviewer
     * @return the completed interview containing the persisted feedback
     */
    @Transactional
    public InterviewerInterviewResponseDTO submitInterviewerFeedback(
            Long interviewId, String feedback, Long expectedVersion, User interviewer) {
        requireInterviewer(interviewer);
        String validatedFeedback = validateFeedback(feedback);
        Interview interview = interviewRepository.findByIdAndInterviewerId(interviewId, interviewer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        requireExpectedVersion(interview, expectedVersion);
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only a scheduled interview can receive feedback");
        }
        interview.setFeedback(validatedFeedback);
        interview.setStatus(InterviewStatus.COMPLETED);
        return InterviewerInterviewResponseDTO.from(interviewRepository.saveAndFlush(interview));
    }

    /**
     * Replaces internal notes on an interview assigned to the interviewer after checking its version.
     * @param interviewId the interview identifier
     * @param notes the internal notes text
     * @param expectedVersion the client-visible version used for optimistic locking
     * @param interviewer the authenticated interviewer
     * @return the interview containing the persisted internal notes
     */
    @Transactional
    public InterviewerInterviewResponseDTO updateInterviewerNotes(
            Long interviewId, String notes, Long expectedVersion, User interviewer) {
        requireInterviewer(interviewer);
        Interview interview = interviewRepository.findByIdAndInterviewerId(interviewId, interviewer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        requireExpectedVersion(interview, expectedVersion);
        String normalized = notes == null ? "" : notes.trim();
        if (normalized.length() > 10000) {
            throw new IllegalArgumentException("Private notes must be at most 10000 characters");
        }
        interview.setInterviewerNotes(normalized.isEmpty() ? null : normalized);
        return InterviewerInterviewResponseDTO.from(interviewRepository.saveAndFlush(interview));
    }

    /**
     * Loads the candidate and application context needed by an assigned interviewer to conduct a review.
     * @param applicationId the application identifier
     * @param interviewer the authenticated interviewer
     * @return the assigned application's candidate-review view
     */
    @Transactional(readOnly = true)
    public InterviewerCandidateReviewDTO getInterviewerCandidateReview(Long applicationId, User interviewer) {
        requireInterviewer(interviewer);
        Application application = applicationRepository.findByIdAndJobCompany(applicationId, interviewer.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        boolean assigned = interviewRepository.existsByApplicationIdAndInterviewerId(applicationId, interviewer.getId());
        boolean reviewableTask = application.getStatus() == ApplicationStatus.TASK_SUBMITTED
                && application.getTaskReviewer() != null
                && Objects.equals(application.getTaskReviewer().getId(), interviewer.getId());
        if (application.getStatus() == ApplicationStatus.TASK_SUBMITTED && !reviewableTask) {
            throw new ResourceNotFoundException("Application not found");
        }
        if (!assigned && !reviewableTask) {
            throw new ResourceNotFoundException("Application not found");
        }
        return InterviewerCandidateReviewDTO.from(application);
    }

    /**
     * Converts an interview entity into the subset of scheduling details visible to a candidate.
     * @param interview the interview being processed
     * @return a candidate-facing interview DTO
     */
    private CandidateInterviewResponseDTO toCandidateResponseDTO(Interview interview) {
        User interviewer = interview.getInterviewer();
        Job job = interview.getApplication().getJob();
        String interviewerName = (interviewer.getFirstName() + " " + interviewer.getLastName()).trim();

        return new CandidateInterviewResponseDTO(
                interview.getId(),
                interview.getApplication().getId(),
                job.getTitle(),
                job.getCompany().getName(),
                interviewerName,
                interview.getScheduledAt(),
                interview.getMeetingLink(),
                interview.getType(),
                interview.getStatus()
        );
    }

    /**
     * Requires the supplied user to have the candidate role.
     * @param candidate the candidate being processed
     */
    private void requireCandidate(User candidate) {
        if (candidate == null || candidate.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Candidate access is required");
        }
    }

    /**
     * Requires the supplied user to be an interviewer assigned to a company.
     * @param interviewer the authenticated interviewer
     */
    private void requireInterviewer(User interviewer) {
        if (interviewer == null || interviewer.getRole() != Role.INTERVIEWER) {
            throw new AccessDeniedException("Interviewer access is required");
        }
        if (interviewer.getCompany() == null) {
            throw new AccessDeniedException("Interviewer must belong to a company");
        }
    }

    /**
     * Requires non-blank interviewer feedback and returns its trimmed form.
     * @param feedback the feedback text
     * @return the validated, trimmed feedback
     */
    private String validateFeedback(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            throw new IllegalArgumentException("Feedback is required");
        }
        String trimmedFeedback = feedback.trim();
        if (trimmedFeedback.length() > 10000) {
            throw new IllegalArgumentException("Feedback must be at most 10000 characters");
        }
        return trimmedFeedback;
    }

    /**
     * Requires an HR user with an assigned company so interview operations remain company-scoped.
     * @param hr the authenticated HR user defining company scope
     */
    private void requireHrWithCompany(User hr) {
        if (hr == null || hr.getRole() != Role.HR) {
            throw new AccessDeniedException("HR access is required");
        }
        if (hr.getCompany() == null) {
            throw new AccessDeniedException("HR user must belong to a company");
        }
    }

    /**
     * Resolves an interview only when its application belongs to the authenticated HR user's company.
     * @param interviewId the interview identifier
     * @param hr the authenticated HR user defining company scope
     * @return the interview within the HR user's company
     */
    private Interview findHrCompanyInterview(Long interviewId, User hr) {
        requireHrWithCompany(hr);
        return interviewRepository.findByIdAndApplicationJobCompany(interviewId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
    }

    /**
     * Verifies that the application workflow and completed prerequisites permit the requested interview type.
     * @param application the application being processed
     * @param type the requested domain type
     */
    private void validateInterviewStage(Application application, InterviewType type) {
        ApplicationStatus status = application.getStatus();
        if (type == InterviewType.HR && status != ApplicationStatus.APPLIED) {
            throw new IllegalArgumentException("HR interview can only be scheduled for an applied application");
        }
        if (type == InterviewType.TECHNICAL
                && status != ApplicationStatus.HR_INTERVIEW
                && status != ApplicationStatus.TASK_APPROVED) {
            throw new IllegalArgumentException(
                    "Technical interview can only be scheduled after the HR interview or an approved home task");
        }
        if (type == InterviewType.TECHNICAL && status == ApplicationStatus.HR_INTERVIEW
                && hasHomeTask(application)) {
            throw new IllegalArgumentException(
                    "Technical interview cannot be scheduled until the home task is approved");
        }
        if (type == InterviewType.MANAGER && status != ApplicationStatus.TECH_INTERVIEW_SCHEDULED) {
            throw new IllegalArgumentException("Manager interview requires a scheduled technical interview stage");
        }
    }

    /**
     * Checks whether the application has a submitted home task approved by its reviewer.
     * @param application the application being processed
     * @return {@code true} when the home task was submitted and approved; otherwise {@code false}
     */
    private boolean hasApprovedHomeTask(Application application) {
        return application.getTaskRepoUrl() != null && !application.getTaskRepoUrl().isBlank();
    }

    /**
     * Checks whether a home task has been assigned to the application.
     * @param application the application being processed
     * @return {@code true} when task instructions are present; otherwise {@code false}
     */
    private boolean hasHomeTask(Application application) {
        return application.getTaskInstructions() != null || application.getTaskDeadline() != null
                || application.getTaskRepoUrl() != null || application.getTaskReviewer() != null;
    }

    /**
     * Requires an HTTP or HTTPS meeting URL and returns its trimmed form.
     * @param value the value to validate or normalize
     * @return the validated, trimmed meeting URL
     */
    private String validateMeetingLink(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Meeting link is required");
        }
        String link = value.trim();
        if (link.length() > 255) {
            throw new IllegalArgumentException("Meeting link must be at most 255 characters");
        }
        try {
            URI uri = new URI(link);
            boolean validScheme = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            if (!validScheme || uri.getHost() == null) {
                throw new IllegalArgumentException("Meeting link must be a valid HTTP or HTTPS URL");
            }
            return link;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Meeting link must be a valid HTTP or HTTPS URL");
        }
    }

    /**
     * Rejects a mutation when the client version is missing or differs from the persisted application version.
     * @param application the application being processed
     * @param expectedVersion the client-visible version used for optimistic locking
     */
    private void requireExpectedVersion(Application application, Long expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("Application version is required");
        }
        if (!expectedVersion.equals(application.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Application.class, application.getId());
        }
    }

    /**
     * Rejects a mutation when the client version is missing or differs from the persisted interview version.
     * @param interview the interview being processed
     * @param expectedVersion the client-visible version used for optimistic locking
     */
    private void requireExpectedVersion(Interview interview, Long expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("Interview version is required");
        }
        if (!expectedVersion.equals(interview.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Interview.class, interview.getId());
        }
    }

    /**
     * Persists an interview and translates database slot collisions into a domain conflict.
     * @param interview the interview being processed
     * @return the persisted interview
     */
    private Interview saveWithSlotConflictMapping(Interview interview) {
        try {
            return interviewRepository.saveAndFlush(interview);
        } catch (DataIntegrityViolationException failure) {
            if (isActiveSlotConflict(failure)) {
                throw new InterviewSlotConflictException(SLOT_CONFLICT_MESSAGE, failure);
            }
            throw failure;
        }
    }

    /**
     * Inspects a persistence failure chain for an active interview-slot constraint violation.
     * @param failure the failure
     * @return {@code true} when the exception chain reports the active-slot uniqueness constraint; otherwise {@code false}
     */
    private boolean isActiveSlotConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintFailure
                    && ACTIVE_SLOT_INDEX.equals(constraintFailure.getConstraintName())) {
                return true;
            }
            if (cause instanceof PSQLException postgresFailure
                    && postgresFailure.getServerErrorMessage() != null
                    && ACTIVE_SLOT_INDEX.equals(postgresFailure.getServerErrorMessage().getConstraint())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Publishes a candidate notification event after the surrounding transaction commits.
     * @param interview the interview being processed
     * @param type the requested domain type
     */
    private void publish(Interview interview, NotificationType type) {
        Application application = interview.getApplication();
        notificationEventPublisher.publishAfterCommit(CandidateNotificationEvent.of(type,
                application.getCandidate().getId(), application.getId(), interview.getId()));
    }
}
