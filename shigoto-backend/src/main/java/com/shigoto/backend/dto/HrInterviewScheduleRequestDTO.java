package com.shigoto.backend.dto;

import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

/**
 * Defines the interviewer, type, time, meeting link, and expected application version for a new interview.
 */
public record HrInterviewScheduleRequestDTO(
        Long interviewerId,
        InterviewType type,
        LocalDateTime scheduledAt,
        String meetingLink,
        Long applicationVersion
) {}
