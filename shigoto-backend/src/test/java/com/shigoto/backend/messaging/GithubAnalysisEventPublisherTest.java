package com.shigoto.backend.messaging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GithubAnalysisEventPublisherTest {
    private final JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    private final GithubAnalysisEventPublisher publisher = new GithubAnalysisEventPublisher(jmsTemplate);
    private final GithubAnalysisRequestedEvent event = GithubAnalysisRequestedEvent.of(1L, 2L, "octocat");

    @BeforeEach
    void initializeTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void publishesOnlyAfterCommit() {
        publisher.publishAfterCommit(event);
        verifyNoInteractions(jmsTemplate);

        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();

        verify(jmsTemplate).convertAndSend(GithubAnalysisEventPublisher.QUEUE, event);
    }

    @Test
    void requiresBusinessTransaction() {
        TransactionSynchronizationManager.clear();
        assertThrows(IllegalStateException.class, () -> publisher.publishAfterCommit(event));
    }
}
