package com.shigoto.backend.dto;

/**
 * Defines final interview feedback and the expected interview version.
 */
public record InterviewerFeedbackRequestDTO(String feedback, Long version) {}
