package com.shigoto.backend.dto;

import com.shigoto.backend.entity.TaskReviewDecision;

public record InterviewerTaskReviewRequestDTO(TaskReviewDecision decision, Long version) {}
