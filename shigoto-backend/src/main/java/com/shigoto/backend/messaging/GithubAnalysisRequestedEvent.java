package com.shigoto.backend.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record GithubAnalysisRequestedEvent(
        UUID eventId,
        Long candidateUserId,
        Long applicationId,
        String githubUsername,
        LocalDateTime occurredAt
) implements Serializable {
    public static GithubAnalysisRequestedEvent of(
            Long candidateUserId, Long applicationId, String githubUsername) {
        return new GithubAnalysisRequestedEvent(UUID.randomUUID(), candidateUserId, applicationId,
                githubUsername, LocalDateTime.now());
    }
}
