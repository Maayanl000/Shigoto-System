package com.shigoto.backend.dto;

import com.shigoto.backend.entity.InterviewType;
import java.time.LocalDateTime;

public record InterviewRequestDTO(
        Long applicationId,
        Long interviewerId,
        LocalDateTime scheduledAt,
        String meetingLink,
        InterviewType type
) {}