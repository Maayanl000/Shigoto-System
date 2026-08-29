package com.shigoto.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component @RequiredArgsConstructor @Slf4j
public class NotificationEventPublisher {
    public static final String QUEUE = "shigoto.notifications";
    private final JmsTemplate jmsTemplate;

    public void publishAfterCommit(CandidateNotificationEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Notification events require an active business transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    jmsTemplate.convertAndSend(QUEUE, event);
                } catch (RuntimeException deliveryFailure) {
                    // The authoritative business transaction is already committed; messaging must not change its result.
                    log.error("Could not publish candidate notification event {} after commit", event.eventId(), deliveryFailure);
                }
            }
        });
    }
}
