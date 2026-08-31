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
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateEmailService {
    private final JavaMailSender mailSender;
    private final CandidateEmailRenderer renderer;
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

        try {
            CandidateEmailRenderer.RenderedEmail rendered = renderer.render(event.type(), candidate, application, interview);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(candidate.getEmail());
            helper.setSubject(rendered.subject());
            helper.setText(rendered.html(), true);
            helper.addInline("shigoto-banner", new ClassPathResource("email/shigoto-banner.png"), "image/png");
            mailSender.send(message);
        } catch (MessagingException preparationFailure) {
            MailPreparationException mailFailure = new MailPreparationException(preparationFailure);
            log.error("Candidate email preparation failed for event {}", event.eventId(), mailFailure);
            throw mailFailure;
        } catch (RuntimeException mailFailure) {
            log.error("Candidate email delivery failed for event {}", event.eventId(), mailFailure);
            throw mailFailure;
        }
    }

}
