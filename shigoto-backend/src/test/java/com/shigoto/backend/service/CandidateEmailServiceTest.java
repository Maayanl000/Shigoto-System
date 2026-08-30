package com.shigoto.backend.service;

import com.shigoto.backend.entity.*;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CandidateEmailServiceTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final InterviewRepository interviewRepository = mock(InterviewRepository.class);
    private final CandidateEmailService service = new CandidateEmailService(
            mailSender, userRepository, applicationRepository, interviewRepository);

    private final User candidate = User.builder().id(1L).email("candidate@example.test")
            .role(Role.CANDIDATE).build();
    private final Job job = Job.builder().id(2L).title("Backend Engineer").build();
    private final Application application = Application.builder().id(3L).candidate(candidate).job(job)
            .hrNotes("PRIVATE_HR").taskReviewNotes("PRIVATE_TASK_REVIEW")
            .taskDeadline(LocalDateTime.of(2026, 9, 5, 17, 0)).build();
    private final Interview interview = Interview.builder().id(4L).application(application)
            .type(InterviewType.TECHNICAL).scheduledAt(LocalDateTime.of(2026, 9, 2, 10, 30))
            .interviewerNotes("PRIVATE_INTERVIEWER").feedback("PRIVATE_FEEDBACK").build();

    @BeforeEach void enableAndResolveReferences() {
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "from", "noreply@shigoto.test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.findById(3L)).thenReturn(Optional.of(application));
        when(interviewRepository.findById(4L)).thenReturn(Optional.of(interview));
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void everySupportedTypeSendsSafeEmail(NotificationType type) {
        Long interviewId = type.name().startsWith("INTERVIEW_") ? 4L : null;
        service.receive(CandidateNotificationEvent.of(type, 1L, 3L, interviewId));

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertArrayEquals(new String[]{"candidate@example.test"}, message.getTo());
        assertEquals("noreply@shigoto.test", message.getFrom());
        assertNotNull(message.getSubject());
        assertTrue(message.getText().contains("Backend Engineer"));
        assertFalse(message.getText().contains("PRIVATE_HR"));
        assertFalse(message.getText().contains("PRIVATE_TASK_REVIEW"));
        assertFalse(message.getText().contains("PRIVATE_INTERVIEWER"));
        assertFalse(message.getText().contains("PRIVATE_FEEDBACK"));
    }

    @Test void disabledListenerDoesNotResolveOrSend() {
        ReflectionTestUtils.setField(service, "emailEnabled", false);
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null));
        verifyNoInteractions(mailSender, userRepository, applicationRepository, interviewRepository);
    }

    @Test void homeTaskUpdateUsesSafeSubjectAndUpdatedDeadline() {
        service.receive(CandidateNotificationEvent.of(NotificationType.HOME_TASK_UPDATED, 1L, 3L, null));

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("Home task deadline updated", captor.getValue().getSubject());
        assertTrue(captor.getValue().getText().contains("Backend Engineer"));
        assertTrue(captor.getValue().getText().contains("2026-09-05 17:00"));
        assertFalse(captor.getValue().getText().contains("PRIVATE_HR"));
        assertFalse(captor.getValue().getText().contains("PRIVATE_TASK_REVIEW"));
    }

    @Test void candidateApplicationMismatchDoesNotSend() {
        application.setCandidate(User.builder().id(99L).build());
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null));
        verifyNoInteractions(mailSender);
    }

    @Test void wrongCandidateRoleDoesNotSend() {
        candidate.setRole(Role.HR);
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null));
        verifyNoInteractions(mailSender);
    }

    @Test void interviewApplicationMismatchDoesNotSend() {
        interview.setApplication(Application.builder().id(99L).build());
        service.receive(CandidateNotificationEvent.of(NotificationType.INTERVIEW_SCHEDULED, 1L, 3L, 4L));
        verifyNoInteractions(mailSender);
    }

    @Test void smtpFailurePropagatesForJmsRedelivery() {
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(mailSender).send(any(SimpleMailMessage.class));

        MailSendException thrown = assertThrows(MailSendException.class, () -> service.receive(
                CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null)));

        assertSame(failure, thrown);
    }
}
