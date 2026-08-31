package com.shigoto.backend.service;

import com.shigoto.backend.dto.CandidateInterviewResponseDTO;
import com.shigoto.backend.dto.HrInterviewerOptionDTO;
import com.shigoto.backend.dto.HrInterviewScheduleRequestDTO;
import com.shigoto.backend.dto.HrInterviewRescheduleRequestDTO;
import com.shigoto.backend.dto.HrScheduledInterviewResponseDTO;
import com.shigoto.backend.dto.InterviewerInterviewResponseDTO;
import com.shigoto.backend.dto.InterviewerCandidateReviewDTO;
import com.shigoto.backend.entity.*;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.messaging.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public HrScheduledInterviewResponseDTO scheduleInterview(
            Long applicationId, HrInterviewScheduleRequestDTO request, User hr) {
        requireHrWithCompany(hr);
        if (request == null || request.interviewerId() == null || request.type() == null
                || request.scheduledAt() == null) {
            throw new IllegalArgumentException("Interviewer, interview type, and scheduled time are required");
        }
        if (!request.scheduledAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Interview must be scheduled in the future");
        }
        String meetingLink = validateMeetingLink(request.meetingLink());

        Application application = applicationRepository.findByIdAndJobCompany(applicationId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (application.getStatus() == ApplicationStatus.OFFER
                || application.getStatus() == ApplicationStatus.HIRED
                || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot schedule an interview for a terminal application");
        }
        validateInterviewStage(application.getStatus(), request.type());

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
        Interview saved = interviewRepository.save(interview);
        publish(saved, NotificationType.INTERVIEW_SCHEDULED);
        return HrScheduledInterviewResponseDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public List<HrInterviewerOptionDTO> getCompanyInterviewers(User hr) {
        requireHrWithCompany(hr);
        return userRepository.findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(Role.INTERVIEWER, hr.getCompany())
                .stream().map(HrInterviewerOptionDTO::from).toList();
    }

    @Transactional
    public HrScheduledInterviewResponseDTO rescheduleInterview(
            Long interviewId, HrInterviewRescheduleRequestDTO request, User hr) {
        requireHrWithCompany(hr);
        if (request == null || request.interviewerId() == null || request.scheduledAt() == null) {
            throw new IllegalArgumentException("Interviewer and scheduled time are required");
        }
        if (!request.scheduledAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Interview must be scheduled in the future");
        }
        String meetingLink = validateMeetingLink(request.meetingLink());
        Interview interview = findHrCompanyInterview(interviewId, hr);
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only a scheduled interview can be rescheduled");
        }
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
        Interview saved = interviewRepository.save(interview);
        publish(saved, NotificationType.INTERVIEW_RESCHEDULED);
        return HrScheduledInterviewResponseDTO.from(saved);
    }

    @Transactional
    public HrScheduledInterviewResponseDTO cancelInterview(Long interviewId, User hr) {
        Interview interview = findHrCompanyInterview(interviewId, hr);
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
                    : ApplicationStatus.HR_INTERVIEW);
            applicationRepository.save(application);
        }
        Interview saved = interviewRepository.save(interview);
        publish(saved, NotificationType.INTERVIEW_CANCELED);
        return HrScheduledInterviewResponseDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public List<HrScheduledInterviewResponseDTO> getHrApplicationInterviews(Long applicationId, User hr) {
        requireHrWithCompany(hr);
        applicationRepository.findByIdAndJobCompany(applicationId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return interviewRepository.findByApplicationIdOrderByScheduledAtAsc(applicationId)
                .stream().map(HrScheduledInterviewResponseDTO::from).toList();
    }

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

    @Transactional(readOnly = true)
    public List<CandidateInterviewResponseDTO> getCandidateInterviews(User candidate) {
        requireCandidate(candidate);
        return interviewRepository.findByApplicationCandidateIdOrderByScheduledAtAsc(candidate.getId())
                .stream()
                .map(this::toCandidateResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewerInterviewResponseDTO> getInterviewerInterviews(User interviewer) {
        requireInterviewer(interviewer);
        return interviewRepository.findByInterviewerIdOrderByScheduledAtAsc(interviewer.getId())
                .stream()
                .map(InterviewerInterviewResponseDTO::from)
                .toList();
    }

    @Transactional
    public InterviewerInterviewResponseDTO submitInterviewerFeedback(
            Long interviewId, String feedback, User interviewer) {
        requireInterviewer(interviewer);
        String validatedFeedback = validateFeedback(feedback);
        Interview interview = interviewRepository.findByIdAndInterviewerId(interviewId, interviewer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only a scheduled interview can receive feedback");
        }
        interview.setFeedback(validatedFeedback);
        interview.setStatus(InterviewStatus.COMPLETED);
        return InterviewerInterviewResponseDTO.from(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewerInterviewResponseDTO updateInterviewerNotes(
            Long interviewId, String notes, User interviewer) {
        requireInterviewer(interviewer);
        Interview interview = interviewRepository.findByIdAndInterviewerId(interviewId, interviewer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        String normalized = notes == null ? "" : notes.trim();
        if (normalized.length() > 10000) {
            throw new IllegalArgumentException("Private notes must be at most 10000 characters");
        }
        interview.setInterviewerNotes(normalized.isEmpty() ? null : normalized);
        return InterviewerInterviewResponseDTO.from(interviewRepository.save(interview));
    }

    @Transactional(readOnly = true)
    public InterviewerCandidateReviewDTO getInterviewerCandidateReview(Long applicationId, User interviewer) {
        requireInterviewer(interviewer);
        Application application = applicationRepository.findByIdAndJobCompany(applicationId, interviewer.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        boolean assigned = interviewRepository.existsByApplicationIdAndInterviewerId(applicationId, interviewer.getId());
        boolean reviewableTask = application.getStatus() == ApplicationStatus.TASK_SUBMITTED;
        if (!assigned && !reviewableTask) {
            throw new ResourceNotFoundException("Application not found");
        }
        return InterviewerCandidateReviewDTO.from(application);
    }

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

    private void requireCandidate(User candidate) {
        if (candidate == null || candidate.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Candidate access is required");
        }
    }

    private void requireInterviewer(User interviewer) {
        if (interviewer == null || interviewer.getRole() != Role.INTERVIEWER) {
            throw new AccessDeniedException("Interviewer access is required");
        }
        if (interviewer.getCompany() == null) {
            throw new AccessDeniedException("Interviewer must belong to a company");
        }
    }

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

    private void requireHrWithCompany(User hr) {
        if (hr == null || hr.getRole() != Role.HR) {
            throw new AccessDeniedException("HR access is required");
        }
        if (hr.getCompany() == null) {
            throw new AccessDeniedException("HR user must belong to a company");
        }
    }

    private Interview findHrCompanyInterview(Long interviewId, User hr) {
        requireHrWithCompany(hr);
        return interviewRepository.findByIdAndApplicationJobCompany(interviewId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
    }

    private void validateInterviewStage(ApplicationStatus status, InterviewType type) {
        if (type == InterviewType.HR && status != ApplicationStatus.APPLIED) {
            throw new IllegalArgumentException("HR interview can only be scheduled for an applied application");
        }
        if (type == InterviewType.TECHNICAL
                && status != ApplicationStatus.HR_INTERVIEW
                && status != ApplicationStatus.TASK_APPROVED) {
            throw new IllegalArgumentException(
                    "Technical interview can only be scheduled after the HR interview or an approved home task");
        }
        if (type == InterviewType.MANAGER && status != ApplicationStatus.TECH_INTERVIEW_SCHEDULED) {
            throw new IllegalArgumentException("Manager interview requires a scheduled technical interview stage");
        }
    }

    private boolean hasApprovedHomeTask(Application application) {
        return application.getTaskRepoUrl() != null && !application.getTaskRepoUrl().isBlank();
    }

    private String validateMeetingLink(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Meeting link is required");
        }
        String link = value.trim();
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

    private void publish(Interview interview, NotificationType type) {
        Application application = interview.getApplication();
        notificationEventPublisher.publishAfterCommit(CandidateNotificationEvent.of(type,
                application.getCandidate().getId(), application.getId(), interview.getId()));
    }
}
