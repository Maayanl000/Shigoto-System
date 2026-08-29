package com.shigoto.backend.messaging;

import com.shigoto.backend.entity.NotificationType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record CandidateNotificationEvent(UUID eventId, NotificationType type, Long candidateUserId,
        Long applicationId, Long interviewId, LocalDateTime occurredAt) implements Serializable {
    public static CandidateNotificationEvent of(NotificationType type, Long candidateId,
            Long applicationId, Long interviewId) {
        return new CandidateNotificationEvent(UUID.randomUUID(), type, candidateId, applicationId,
                interviewId, LocalDateTime.now());
    }
}
