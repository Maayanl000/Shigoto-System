package com.shigoto.backend.dto;

/**
 * Defines the candidate's task repository URL and expected application version.
 */
public record TaskSubmissionRequestDTO(String repositoryUrl, Long version) {
}
