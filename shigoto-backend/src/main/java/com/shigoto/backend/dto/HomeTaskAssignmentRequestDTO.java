package com.shigoto.backend.dto;

import java.time.LocalDateTime;

public record HomeTaskAssignmentRequestDTO(
        String taskInstructions,
        LocalDateTime deadline,
        Long reviewerId,
        Long version
) {}
