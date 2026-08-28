package com.shigoto.backend.dto;

import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

public record HrInterviewScheduleRequestDTO(
        Long interviewerId,
        InterviewType type,
        LocalDateTime scheduledAt,
        String meetingLink
) {}
