package com.shigoto.backend.dto;

import java.time.LocalDateTime;

public record HrInterviewRescheduleRequestDTO(
        Long interviewerId,
        LocalDateTime scheduledAt,
        String meetingLink,
        Long version
) {}
