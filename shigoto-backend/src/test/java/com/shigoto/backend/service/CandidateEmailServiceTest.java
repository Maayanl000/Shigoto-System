package com.shigoto.backend.service;

import com.shigoto.backend.entity.*;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailSendException;
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
    private final CandidateEmailRenderer renderer = new CandidateEmailRenderer();
    private final CandidateEmailService service = new CandidateEmailService(
            mailSender, renderer, userRepository, applicationRepository, interviewRepository);

    private final User candidate = User.builder().id(1L).firstName("Dana").email("candidate@example.test")
            .role(Role.CANDIDATE).build();
    private final Job job = Job.builder().id(2L).title("Backend Engineer").build();
    private final Application application = Application.builder().id(3L).candidate(candidate).job(job)
            .hrNotes("PRIVATE_HR").taskReviewNotes("PRIVATE_TASK_REVIEW")
            .taskInstructions("Build a safe API")
            .taskDeadline(LocalDateTime.of(2026, 9, 5, 17, 0)).build();
    private final Interview interview = Interview.builder().id(4L).application(application)
            .type(InterviewType.TECHNICAL).scheduledAt(LocalDateTime.of(2026, 9, 2, 10, 30))
            .meetingLink("https://meet.example.com/interview")
            .interviewerNotes("PRIVATE_INTERVIEWER").feedback("PRIVATE_FEEDBACK").build();

    @BeforeEach void enableAndResolveReferences() {
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "from", "noreply@shigoto.test");
        ReflectionTestUtils.setField(renderer, "frontendUrl", "https://shigoto.example/");
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        when(userRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.findById(3L)).thenReturn(Optional.of(application));
        when(interviewRepository.findById(4L)).thenReturn(Optional.of(interview));
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void everySupportedTypeSendsBrandedSafeHtml(NotificationType type) throws Exception {
        Long interviewId = type.name().startsWith("INTERVIEW_") ? 4L : null;
        service.receive(CandidateNotificationEvent.of(type, 1L, 3L, interviewId));

        MimeMessage message = sentMessage();
        String html = htmlBody(message);
        assertEquals("candidate@example.test", message.getAllRecipients()[0].toString());
        assertEquals("noreply@shigoto.test", message.getFrom()[0].toString());
        assertNotNull(message.getSubject());
        assertTrue(html.contains("Hi Dana,"));
        assertTrue(html.contains("Backend Engineer"));
        assertTrue(html.contains("cid:shigoto-banner"));
        assertTrue(hasContentId(message, "<shigoto-banner>"));
        assertTrue(html.contains("https://shigoto.example/candidate/applications/3"));
        assertFalse(html.contains("PRIVATE_HR"));
        assertFalse(html.contains("PRIVATE_TASK_REVIEW"));
        assertFalse(html.contains("PRIVATE_INTERVIEWER"));
        assertFalse(html.contains("PRIVATE_FEEDBACK"));
    }

    @Test void taskAndInterviewContentUsesReadableDatesAndSafeLinks() throws Exception {
        application.setTaskInstructions("First line\n<script>alert('x')</script>");
        service.receive(CandidateNotificationEvent.of(NotificationType.HOME_TASK_ASSIGNED, 1L, 3L, null));
        String taskHtml = htmlBody(sentMessage());
        assertTrue(taskHtml.contains("Sep 5, 2026 at 5:00 PM"));
        assertTrue(taskHtml.contains("First line<br>&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"));
        assertFalse(taskHtml.contains("<script>"));

        clearInvocations(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        service.receive(CandidateNotificationEvent.of(NotificationType.INTERVIEW_SCHEDULED, 1L, 3L, 4L));
        String interviewHtml = htmlBody(sentMessage());
        assertTrue(interviewHtml.contains("Sep 2, 2026 at 10:30 AM"));
        assertTrue(interviewHtml.contains("Join meeting"));
        assertTrue(interviewHtml.contains("https://meet.example.com/interview"));
    }

    @Test void invalidMeetingLinkAndCanceledInterviewOmitJoinButton() throws Exception {
        interview.setMeetingLink("javascript:alert(1)");
        service.receive(CandidateNotificationEvent.of(NotificationType.INTERVIEW_SCHEDULED, 1L, 3L, 4L));
        assertFalse(htmlBody(sentMessage()).contains("Join meeting"));

        clearInvocations(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        interview.setMeetingLink("https://meet.example.com/interview");
        service.receive(CandidateNotificationEvent.of(NotificationType.INTERVIEW_CANCELED, 1L, 3L, 4L));
        assertFalse(htmlBody(sentMessage()).contains("Join meeting"));
    }

    @Test void rejectionUsesEscapedCandidateFeedbackOrGenericFallback() throws Exception {
        application.setCandidateFeedback("Thank you <script>bad()</script>\nKeep learning.");
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null));
        String feedbackHtml = htmlBody(sentMessage());
        assertTrue(feedbackHtml.contains("Feedback from the recruitment team"));
        assertTrue(feedbackHtml.contains("&lt;script&gt;bad()&lt;/script&gt;<br>Keep learning."));
        assertFalse(feedbackHtml.contains("<script>"));

        clearInvocations(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        application.setCandidateFeedback("   ");
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null));
        assertTrue(htmlBody(sentMessage()).contains("continue with other candidates"));
    }

    @Test void offerAndHiredEmailsUseExactSafeMilestoneContent() throws Exception {
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_OFFERED, 1L, 3L, null));
        MimeMessage offered = sentMessage();
        assertEquals("Great news - offer update", offered.getSubject());
        assertTrue(htmlBody(offered).contains(
                "The company would like to move forward with an offer for <strong>Backend Engineer</strong>."));

        clearInvocations(mailSender);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_HIRED, 1L, 3L, null));
        MimeMessage hired = sentMessage();
        assertEquals("Congratulations - hiring update", hired.getSubject());
        String hiredHtml = htmlBody(hired);
        assertTrue(hiredHtml.contains("Hi Dana,"));
        assertTrue(hiredHtml.contains(
                "Your application for <strong>Backend Engineer</strong> has been marked as hired."));
        assertFalse(hiredHtml.contains("PRIVATE_HR"));
        assertFalse(hiredHtml.contains("PRIVATE_TASK_REVIEW"));
        assertFalse(hiredHtml.contains("salary"));
        assertFalse(hiredHtml.contains("start date"));
    }

    @Test void applicationSubmissionEmailUsesBrandedConfirmationContent() throws Exception {
        job.setCompany(Company.builder().name("Shigoto Labs").build());

        service.receive(CandidateNotificationEvent.of(
                NotificationType.APPLICATION_SUBMITTED, 1L, 3L, null));

        MimeMessage submitted = sentMessage();
        assertEquals("Application received", submitted.getSubject());
        String html = htmlBody(submitted);
        assertTrue(html.contains("Thank you for applying"));
        assertTrue(html.contains("We received your application for <strong>Backend Engineer</strong> at "
                + "<strong>Shigoto Labs</strong>."));
        assertTrue(html.contains("We will keep you updated as your application progresses."));
        assertTrue(html.contains("View application"));
        assertFalse(html.contains("PRIVATE_HR"));
        assertFalse(html.contains("PRIVATE_TASK_REVIEW"));
    }

    @Test void blankNameAndInvalidFrontendUrlUseFallbackAndOmitCta() throws Exception {
        candidate.setFirstName(" ");
        ReflectionTestUtils.setField(renderer, "frontendUrl", "javascript:alert(1)");
        service.receive(CandidateNotificationEvent.of(NotificationType.HOME_TASK_UPDATED, 1L, 3L, null));
        String html = htmlBody(sentMessage());
        assertTrue(html.contains("Hello,"));
        assertFalse(html.contains("View application"));
    }

    @Test void disabledListenerDoesNotResolveOrSend() {
        ReflectionTestUtils.setField(service, "emailEnabled", false);
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null));
        verifyNoInteractions(mailSender, userRepository, applicationRepository, interviewRepository);
    }

    @Test void candidateApplicationAndInterviewMismatchesDoNotSend() {
        application.setCandidate(User.builder().id(99L).build());
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null));
        verify(mailSender, never()).send(any(MimeMessage.class));

        application.setCandidate(candidate);
        interview.setApplication(Application.builder().id(99L).build());
        service.receive(CandidateNotificationEvent.of(NotificationType.INTERVIEW_SCHEDULED, 1L, 3L, 4L));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test void smtpFailurePropagatesForJmsRedelivery() {
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(mailSender).send(any(MimeMessage.class));
        MailSendException thrown = assertThrows(MailSendException.class, () -> service.receive(
                CandidateNotificationEvent.of(NotificationType.APPLICATION_REJECTED, 1L, 3L, null)));
        assertSame(failure, thrown);
    }

    @Test void bannerResourceIsPackaged() {
        assertTrue(new ClassPathResource("email/shigoto-banner.png").exists());
    }

    private MimeMessage sentMessage() {
        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private String htmlBody(MimeMessage message) throws Exception {
        return findHtml(message.getContent());
    }

    private String findHtml(Object content) throws Exception {
        if (content instanceof String text) return text;
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/html")) return (String) part.getContent();
                String nested = findHtml(part.getContent());
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private boolean hasContentId(MimeMessage message, String expected) throws Exception {
        return hasContentId(message.getContent(), expected);
    }

    private boolean hasContentId(Object content, String expected) throws Exception {
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                String[] contentIds = part.getHeader("Content-ID");
                if (contentIds != null && java.util.Arrays.asList(contentIds).contains(expected)) return true;
                if (hasContentId(part.getContent(), expected)) return true;
            }
        }
        return false;
    }
}
