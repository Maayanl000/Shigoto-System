package com.shigoto.backend.entity;

/**
 * Defines the supported interview status domain values.
 */
public enum InterviewStatus {
    SCHEDULED,  // Awaiting the scheduled meeting.
    COMPLETED,  // Completed with interviewer feedback.
    CANCELED    // Canceled before completion.
}
