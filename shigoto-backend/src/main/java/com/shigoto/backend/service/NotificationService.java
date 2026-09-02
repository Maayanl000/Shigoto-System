package com.shigoto.backend.service;

import com.shigoto.backend.dto.NotificationResponseDTO;
import com.shigoto.backend.entity.*;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service @RequiredArgsConstructor @Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    @JmsListener(destination = "shigoto.notifications")
    @Transactional
    public void receive(CandidateNotificationEvent event) {
        if (event == null || event.eventId() == null || event.type() == null
                || event.candidateUserId() == null || event.applicationId() == null) {
            log.warn("Ignoring malformed candidate notification event");
            return;
        }
        if (notificationRepository.existsByEventId(event.eventId())) return;
        User candidate = userRepository.findById(event.candidateUserId()).orElse(null);
        Application application = applicationRepository.findById(event.applicationId()).orElse(null);
        if (candidate == null || candidate.getRole() != Role.CANDIDATE || application == null
                || !Objects.equals(application.getCandidate().getId(), candidate.getId())) {
            log.warn("Ignoring notification event {} with missing or mismatched references", event.eventId());
            return;
        }
        if (event.interviewId() != null) {
            var interview = interviewRepository.findById(event.interviewId()).orElse(null);
            if (interview == null || !Objects.equals(interview.getApplication().getId(), application.getId())) {
                log.warn("Ignoring notification event {} with invalid interview", event.eventId());
                return;
            }
        }
        String jobTitle = application.getJob().getTitle();
        String candidateFirstName = candidate.getFirstName() == null ? "" : candidate.getFirstName().trim();
        String title = switch (event.type()) {
            case APPLICATION_SUBMITTED -> "Application received";
            case APPLICATION_REJECTED -> "Application update";
            case APPLICATION_OFFERED -> "Offer update";
            case APPLICATION_HIRED -> "Hiring update";
            case HOME_TASK_ASSIGNED -> "New home task";
            case HOME_TASK_UPDATED -> "Home task deadline updated";
            case INTERVIEW_SCHEDULED -> "Interview scheduled";
            case INTERVIEW_RESCHEDULED -> "Interview rescheduled";
            case INTERVIEW_CANCELED -> "Interview canceled";
        };
        String message = switch (event.type()) {
            case APPLICATION_SUBMITTED -> "Your application for " + jobTitle + " at "
                    + application.getJob().getCompany().getName() + " was submitted successfully.";
            case APPLICATION_REJECTED -> "Your application for " + jobTitle + " was not selected.";
            case APPLICATION_OFFERED -> (candidateFirstName.isEmpty() ? "The" : candidateFirstName + ", the")
                    + " company would like to move forward with an offer for " + jobTitle + ".";
            case APPLICATION_HIRED -> (candidateFirstName.isEmpty()
                    ? "Congratulations!" : "Congratulations, " + candidateFirstName + "!")
                    + " Your application for " + jobTitle
                    + " has been marked as hired.";
            case HOME_TASK_ASSIGNED -> "A home task was assigned for " + jobTitle + ".";
            case HOME_TASK_UPDATED -> "The home task deadline for " + jobTitle + " was updated"
                    + (application.getTaskDeadline() == null ? "."
                    : " to " + application.getTaskDeadline() + ".");
            case INTERVIEW_SCHEDULED -> "An interview was scheduled for " + jobTitle + ".";
            case INTERVIEW_RESCHEDULED -> "Your interview for " + jobTitle + " was rescheduled.";
            case INTERVIEW_CANCELED -> "Your interview for " + jobTitle + " was canceled.";
        };
        try {
            notificationRepository.saveAndFlush(Notification.builder().eventId(event.eventId()).recipient(candidate)
                    .type(event.type()).title(title).message(message).applicationId(application.getId())
                    .interviewId(event.interviewId()).createdAt(event.occurredAt()).build());
        } catch (DataIntegrityViolationException duplicateDelivery) {
            log.debug("Notification event {} was already persisted", event.eventId());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> mine(User candidate) {
        requireCandidate(candidate);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(candidate.getId())
                .stream().map(NotificationResponseDTO::from).toList();
    }

    @Transactional
    public NotificationResponseDTO markRead(Long id, User candidate) {
        requireCandidate(candidate);
        Notification notification = notificationRepository.findByIdAndRecipientId(id, candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) notification.setReadAt(LocalDateTime.now());
        return NotificationResponseDTO.from(notification);
    }

    private void requireCandidate(User user) {
        if (user == null || user.getRole() != Role.CANDIDATE) throw new AccessDeniedException("Candidate access is required");
    }
}
