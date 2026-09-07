package com.shigoto.backend.messaging;

import com.shigoto.backend.entity.NotificationType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Carries immutable candidate notification event data between transactional message producers and consumers.
 */
public record CandidateNotificationEvent(UUID eventId, NotificationType type, Long candidateUserId,
        Long applicationId, Long interviewId, LocalDateTime occurredAt) implements Serializable {
    /**
     * Creates an immutable event with a unique identifier and current occurrence time.
     * @param type the requested domain type
     * @param candidateId the candidate identifier
     * @param applicationId the application identifier
     * @param interviewId the interview identifier
     * @return a new event with a generated identifier and current occurrence time
     */
    public static CandidateNotificationEvent of(NotificationType type, Long candidateId,
            Long applicationId, Long interviewId) {
        return new CandidateNotificationEvent(UUID.randomUUID(), type, candidateId, applicationId,
                interviewId, LocalDateTime.now());
    }
}
