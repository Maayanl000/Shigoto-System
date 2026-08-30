package com.shigoto.backend.service;

import com.shigoto.backend.entity.*;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateEmailService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    @Value("${shigoto.email.enabled:false}")
    private boolean emailEnabled;
    @Value("${shigoto.email.from:}")
    private String from;

    /** JMS redelivery makes email delivery at-least-once; consumers must tolerate possible duplicates. */
    @JmsListener(destination = "shigoto.emails")
    @Transactional(readOnly = true)
    public void receive(CandidateNotificationEvent event) {
        if (!emailEnabled) return;
        if (event == null || event.eventId() == null || event.type() == null
                || event.candidateUserId() == null || event.applicationId() == null) {
            log.warn("Ignoring malformed candidate email event");
            return;
        }

        User candidate = userRepository.findById(event.candidateUserId()).orElse(null);
        Application application = applicationRepository.findById(event.applicationId()).orElse(null);
        if (candidate == null || candidate.getRole() != Role.CANDIDATE || application == null
                || application.getCandidate() == null
                || !Objects.equals(application.getCandidate().getId(), candidate.getId())) {
            log.warn("Ignoring candidate email event {} with missing or mismatched references", event.eventId());
            return;
        }

        Interview interview = null;
        if (event.interviewId() != null) {
            interview = interviewRepository.findById(event.interviewId()).orElse(null);
            if (interview == null || interview.getApplication() == null
                    || !Objects.equals(interview.getApplication().getId(), application.getId())) {
                log.warn("Ignoring candidate email event {} with invalid interview", event.eventId());
                return;
            }
        }

        SimpleMailMessage message = buildMessage(event.type(), candidate, application, interview);
        try {
            mailSender.send(message);
        } catch (RuntimeException mailFailure) {
            log.error("Candidate email delivery failed for event {}", event.eventId(), mailFailure);
            throw mailFailure;
        }
    }

    private SimpleMailMessage buildMessage(NotificationType type, User candidate, Application application,
                                           Interview interview) {
        String jobTitle = application.getJob().getTitle();
        String[] content = switch (type) {
            case HOME_TASK_ASSIGNED -> new String[]{"Home task assigned",
                "A home task was assigned for " + jobTitle + "."
                        + (application.getTaskDeadline() == null ? ""
                        : " Deadline: " + DATE_TIME.format(application.getTaskDeadline()) + ".")};
            case HOME_TASK_UPDATED -> new String[]{"Home task deadline updated",
                    "The home task deadline for " + jobTitle + " was updated"
                            + (application.getTaskDeadline() == null ? "."
                            : " to " + DATE_TIME.format(application.getTaskDeadline()) + ".")};
            case INTERVIEW_SCHEDULED -> new String[]{"Interview scheduled",
                    interviewText("An interview was scheduled", jobTitle, interview)};
            case INTERVIEW_RESCHEDULED -> new String[]{"Interview rescheduled",
                    interviewText("Your interview was rescheduled", jobTitle, interview)};
            case INTERVIEW_CANCELED -> new String[]{"Interview canceled",
                    interviewText("Your interview was canceled", jobTitle, interview)};
            case APPLICATION_REJECTED -> new String[]{"Application update",
                    "Thank you for your interest in " + jobTitle
                            + ". We will not be moving forward with your application."};
        };
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(candidate.getEmail());
        message.setSubject(content[0]);
        message.setText(content[1]);
        return message;
    }

    private String interviewText(String action, String jobTitle, Interview interview) {
        if (interview == null) return action + " for " + jobTitle + ".";
        return action + " for " + jobTitle + ". Type: " + interview.getType()
                + ". Date/time: " + DATE_TIME.format(interview.getScheduledAt()) + ".";
    }
}
