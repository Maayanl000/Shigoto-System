package com.shigoto.backend.service;

import com.shigoto.backend.entity.*;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    private NotificationRepository notifications;
    private UserRepository users;
    private ApplicationRepository applications;
    private InterviewRepository interviews;
    private NotificationService service;

    @BeforeEach void setUp() {
        notifications = mock(NotificationRepository.class);
        users = mock(UserRepository.class);
        applications = mock(ApplicationRepository.class);
        interviews = mock(InterviewRepository.class);
        service = new NotificationService(notifications, users, applications, interviews);
    }

    @Test void listenerCreatesSafeCandidateNotificationWithReferences() {
        User candidate = User.builder().id(3L).role(Role.CANDIDATE).build();
        Application application = Application.builder().id(7L).candidate(candidate)
                .job(Job.builder().title("Backend Developer").build()).build();
        Interview interview = Interview.builder().id(9L).application(application).build();
        var event = CandidateNotificationEvent.of(NotificationType.INTERVIEW_SCHEDULED, 3L, 7L, 9L);
        when(users.findById(3L)).thenReturn(Optional.of(candidate));
        when(applications.findById(7L)).thenReturn(Optional.of(application));
        when(interviews.findById(9L)).thenReturn(Optional.of(interview));

        service.receive(event);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).saveAndFlush(saved.capture());
        assertEquals(candidate, saved.getValue().getRecipient());
        assertEquals(7L, saved.getValue().getApplicationId());
        assertEquals(9L, saved.getValue().getInterviewId());
        assertEquals("Interview scheduled", saved.getValue().getTitle());
        assertFalse(saved.getValue().getMessage().contains("feedback"));
    }

    @Test void listenerSafelyIgnoresMissingReferenceAndDuplicateDelivery() {
        var missing = CandidateNotificationEvent.of(NotificationType.HOME_TASK_ASSIGNED, 3L, 7L, null);
        service.receive(missing);
        verify(notifications, never()).saveAndFlush(any());

        clearInvocations(users, applications, interviews);
        when(notifications.existsByEventId(missing.eventId())).thenReturn(true);
        service.receive(missing);
        verifyNoInteractions(users, applications, interviews);
    }

    @Test void homeTaskUpdatePersistsSafeDeadlineWording() {
        User candidate = User.builder().id(3L).role(Role.CANDIDATE).build();
        Application application = Application.builder().id(7L).candidate(candidate)
                .job(Job.builder().title("Backend Developer").build())
                .taskDeadline(LocalDateTime.of(2026, 9, 8, 17, 0))
                .hrNotes("PRIVATE_HR").taskReviewNotes("PRIVATE_REVIEW").build();
        when(users.findById(3L)).thenReturn(Optional.of(candidate));
        when(applications.findById(7L)).thenReturn(Optional.of(application));

        service.receive(CandidateNotificationEvent.of(NotificationType.HOME_TASK_UPDATED, 3L, 7L, null));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).saveAndFlush(saved.capture());
        assertEquals("Home task deadline updated", saved.getValue().getTitle());
        assertTrue(saved.getValue().getMessage().contains("Backend Developer"));
        assertTrue(saved.getValue().getMessage().contains("2026-09-08T17:00"));
        assertFalse(saved.getValue().getMessage().contains("PRIVATE_HR"));
        assertFalse(saved.getValue().getMessage().contains("PRIVATE_REVIEW"));
    }

    @Test void offerAndHiredMilestonesPersistCandidateSafeNotifications() {
        User candidate = User.builder().id(3L).firstName("Dana").role(Role.CANDIDATE).build();
        Application application = Application.builder().id(7L).candidate(candidate)
                .job(Job.builder().title("Backend Developer").build())
                .hrNotes("PRIVATE_HR_SENTINEL").taskReviewNotes("PRIVATE_REVIEW_SENTINEL").build();
        when(users.findById(3L)).thenReturn(Optional.of(candidate));
        when(applications.findById(7L)).thenReturn(Optional.of(application));

        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_OFFERED, 3L, 7L, null));
        service.receive(CandidateNotificationEvent.of(NotificationType.APPLICATION_HIRED, 3L, 7L, null));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).saveAndFlush(saved.capture());
        Notification offered = saved.getAllValues().get(0);
        Notification hired = saved.getAllValues().get(1);
        assertEquals("Offer update", offered.getTitle());
        assertTrue(offered.getMessage().contains("move forward with an offer for Backend Developer"));
        assertTrue(offered.getMessage().startsWith("Dana,"));
        assertEquals("Hiring update", hired.getTitle());
        assertTrue(hired.getMessage().contains("Congratulations, Dana!"));
        assertTrue(hired.getMessage().contains("Backend Developer"));
        for (Notification notification : saved.getAllValues()) {
            assertFalse(notification.getMessage().contains("PRIVATE_HR_SENTINEL"));
            assertFalse(notification.getMessage().contains("PRIVATE_REVIEW_SENTINEL"));
        }
    }

    @Test void applicationSubmissionPersistsCandidateSafeNotification() {
        User candidate = User.builder().id(3L).role(Role.CANDIDATE).build();
        Application application = Application.builder().id(7L).candidate(candidate)
                .job(Job.builder().title("Backend Developer")
                        .company(Company.builder().name("Shigoto Labs").build()).build())
                .hrNotes("PRIVATE_HR_SENTINEL").taskReviewNotes("PRIVATE_REVIEW_SENTINEL").build();
        when(users.findById(3L)).thenReturn(Optional.of(candidate));
        when(applications.findById(7L)).thenReturn(Optional.of(application));

        service.receive(CandidateNotificationEvent.of(
                NotificationType.APPLICATION_SUBMITTED, 3L, 7L, null));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).saveAndFlush(saved.capture());
        assertEquals("Application received", saved.getValue().getTitle());
        assertEquals("Your application for Backend Developer at Shigoto Labs was submitted successfully.",
                saved.getValue().getMessage());
        assertFalse(saved.getValue().getMessage().contains("PRIVATE_HR_SENTINEL"));
        assertFalse(saved.getValue().getMessage().contains("PRIVATE_REVIEW_SENTINEL"));
    }

    @Test void mineIsNewestFirstAndReadIsRecipientScopedAndIdempotent() {
        User candidate = User.builder().id(3L).role(Role.CANDIDATE).build();
        Notification newest = notification(2L, candidate, LocalDateTime.now());
        Notification older = notification(1L, candidate, LocalDateTime.now().minusDays(1));
        when(notifications.findByRecipientIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(newest, older));
        when(notifications.findByIdAndRecipientId(2L, 3L)).thenReturn(Optional.of(newest));

        assertEquals(List.of(2L, 1L), service.mine(candidate).stream().map(n -> n.notificationId()).toList());
        assertTrue(service.markRead(2L, candidate).read());
        LocalDateTime firstRead = newest.getReadAt();
        service.markRead(2L, candidate);
        assertEquals(firstRead, newest.getReadAt());

        User other = User.builder().id(4L).role(Role.CANDIDATE).build();
        assertThrows(com.shigoto.backend.exception.ResourceNotFoundException.class, () -> service.markRead(2L, other));
        verify(notifications, times(2)).findByIdAndRecipientId(2L, 3L);
        verify(notifications).findByIdAndRecipientId(2L, 4L);
    }

    private Notification notification(Long id, User recipient, LocalDateTime createdAt) {
        return Notification.builder().id(id).eventId(UUID.randomUUID()).recipient(recipient)
                .type(NotificationType.APPLICATION_REJECTED).title("Application update")
                .message("Your application was not selected.").applicationId(7L).createdAt(createdAt).build();
    }
}
