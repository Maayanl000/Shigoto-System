package com.shigoto.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class GithubAnalysisEventPublisher {
    public static final String QUEUE = "shigoto.github-analysis";

    private final JmsTemplate jmsTemplate;

    public void publishAfterCommit(GithubAnalysisRequestedEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("GitHub analysis events require an active business transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    jmsTemplate.convertAndSend(QUEUE, event);
                    log.info("Published GitHub analysis event {} for application {}",
                            event.eventId(), event.applicationId());
                } catch (RuntimeException deliveryFailure) {
                    log.error("Could not publish GitHub analysis event {} after commit", event.eventId());
                }
            }
        });
    }
}
