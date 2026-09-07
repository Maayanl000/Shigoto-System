package com.shigoto.backend.entity;

/**
 * Defines the supported application status domain values.
 */
public enum ApplicationStatus {
    APPLIED,                    // Initial state after submission.
    HR_INTERVIEW,               // HR screening is in progress.
    TASK_SENT,                  // A home task has been assigned.
    TASK_SUBMITTED,             // Submission triggers interviewer notification.
    TASK_APPROVED,              // Approval triggers HR notification.
    TECH_INTERVIEW_SCHEDULED,   // A technical interview has been scheduled.
    OFFER,                      // The application reached an offer.
    HIRED,                      // candidate was hired
    REJECTED                    // The application was rejected.
}
