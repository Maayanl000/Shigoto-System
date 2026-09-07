package com.shigoto.backend.dto;

/**
 * Defines candidate-facing feedback and the expected application version.
 */
public record HrCandidateFeedbackRequestDTO(String candidateFeedback, Long version) {}
