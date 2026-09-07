package com.shigoto.backend.dto;

import java.time.LocalDateTime;

/**
 * Defines the home-task instructions, deadline, reviewer, and expected application version submitted by HR.
 */
public record HomeTaskAssignmentRequestDTO(
        String taskInstructions,
        LocalDateTime deadline,
        Long reviewerId,
        Long version
) {}
