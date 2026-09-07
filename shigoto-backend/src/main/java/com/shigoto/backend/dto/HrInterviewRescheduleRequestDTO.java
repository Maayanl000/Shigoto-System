package com.shigoto.backend.dto;

import java.time.LocalDateTime;

/**
 * Defines replacement interview scheduling details and the expected interview version.
 */
public record HrInterviewRescheduleRequestDTO(
        Long interviewerId,
        LocalDateTime scheduledAt,
        String meetingLink,
        Long version
) {}
