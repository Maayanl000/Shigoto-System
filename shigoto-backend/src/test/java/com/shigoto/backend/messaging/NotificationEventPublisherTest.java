package com.shigoto.backend.messaging;

import com.shigoto.backend.entity.NotificationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.*;

class NotificationEventPublisherTest {
    private final JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    private final NotificationEventPublisher publisher = new NotificationEventPublisher(jmsTemplate);
    private final CandidateNotificationEvent event = CandidateNotificationEvent.of(
            NotificationType.APPLICATION_REJECTED, 1L, 2L, null);

    @BeforeEach void initializeTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clear();
    }

    @Test void publishesNotificationAndEnabledEmailAfterCommit() {
        ReflectionTestUtils.setField(publisher, "emailEnabled", true);
        publisher.publishAfterCommit(event);

        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();

        verify(jmsTemplate).convertAndSend(NotificationEventPublisher.QUEUE, event);
        verify(jmsTemplate).convertAndSend(NotificationEventPublisher.EMAIL_QUEUE, event);
    }

    @Test void doesNotPublishEmailWhenDisabled() {
        ReflectionTestUtils.setField(publisher, "emailEnabled", false);
        publisher.publishAfterCommit(event);

        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();

        verify(jmsTemplate).convertAndSend(NotificationEventPublisher.QUEUE, event);
        verify(jmsTemplate, never()).convertAndSend(NotificationEventPublisher.EMAIL_QUEUE, event);
    }

    @Test void notificationFailureDoesNotPreventEmailAttempt() {
        ReflectionTestUtils.setField(publisher, "emailEnabled", true);
        doThrow(new RuntimeException("broker failure")).when(jmsTemplate)
                .convertAndSend(NotificationEventPublisher.QUEUE, event);
        publisher.publishAfterCommit(event);

        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();

        verify(jmsTemplate).convertAndSend(NotificationEventPublisher.EMAIL_QUEUE, event);
    }

    @Test void emailFailureDoesNotEscapeAfterCommitOrAffectNotificationAttempt() {
        ReflectionTestUtils.setField(publisher, "emailEnabled", true);
        doThrow(new RuntimeException("broker failure")).when(jmsTemplate)
                .convertAndSend(NotificationEventPublisher.EMAIL_QUEUE, event);
        publisher.publishAfterCommit(event);

        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();

        verify(jmsTemplate).convertAndSend(NotificationEventPublisher.QUEUE, event);
        verify(jmsTemplate).convertAndSend(NotificationEventPublisher.EMAIL_QUEUE, event);
    }
}
