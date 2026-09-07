package com.shigoto.backend.dto;

import com.shigoto.backend.entity.TaskReviewDecision;

/**
 * Defines an interviewer's task decision and the expected application version.
 */
public record InterviewerTaskReviewRequestDTO(TaskReviewDecision decision, Long version) {}
