package com.shigoto.backend.messaging;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component @RequiredArgsConstructor @Slf4j
public class NotificationEventPublisher {
    public static final String QUEUE = "shigoto.notifications";
    public static final String EMAIL_QUEUE = "shigoto.emails";
    private final JmsTemplate jmsTemplate;
    @Value("${shigoto.email.enabled:false}")
    private boolean emailEnabled;

    @PostConstruct
    void logEmailPublicationConfiguration() {
        log.info("Candidate email JMS publication enabled: {}", emailEnabled);
    }

    public void publishAfterCommit(CandidateNotificationEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Notification events require an active business transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                log.info("Candidate event {} type {}: notification JMS publication to {} attempted; email enabled: {}",
                        event.eventId(), event.type(), QUEUE, emailEnabled);
                publish(QUEUE, event, "candidate notification");
                if (emailEnabled) {
                    log.info("Candidate event {} type {}: email JMS publication to {} attempted; email enabled: true",
                            event.eventId(), event.type(), EMAIL_QUEUE);
                    publish(EMAIL_QUEUE, event, "candidate email");
                } else {
                    log.info("Candidate event {} type {}: email JMS publication to {} skipped; email enabled: false",
                            event.eventId(), event.type(), EMAIL_QUEUE);
                }
            }

            private void publish(String destination, CandidateNotificationEvent event, String description) {
                try {
                    jmsTemplate.convertAndSend(destination, event);
                    log.info("Candidate event {} type {}: {} JMS publication to {} succeeded; email enabled: {}",
                            event.eventId(), event.type(), description, destination, emailEnabled);
                } catch (RuntimeException deliveryFailure) {
                    // The authoritative business transaction is already committed; messaging must not change its result.
                    log.error("Could not publish {} event {} after commit", description, event.eventId(), deliveryFailure);
                }
            }
        });
    }
}
