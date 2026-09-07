package com.shigoto.backend.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Carries an immutable GitHub analysis request between transactional message producers and consumers.
 */
public record GithubAnalysisRequestedEvent(
        UUID eventId,
        Long candidateUserId,
        Long applicationId,
        String githubUsername,
        LocalDateTime occurredAt
) implements Serializable {
    /**
     * Creates an immutable event with a unique identifier and current occurrence time.
     * @param candidateUserId the candidate user id
     * @param applicationId the application identifier
     * @param githubUsername the github username
     * @return a new event with a generated identifier and current occurrence time
     */
    public static GithubAnalysisRequestedEvent of(
            Long candidateUserId, Long applicationId, String githubUsername) {
        return new GithubAnalysisRequestedEvent(UUID.randomUUID(), candidateUserId, applicationId,
                githubUsername, LocalDateTime.now());
    }
}
