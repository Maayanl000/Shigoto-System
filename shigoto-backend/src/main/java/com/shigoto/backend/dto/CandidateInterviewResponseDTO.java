package com.shigoto.backend.dto;

import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

public record CandidateInterviewResponseDTO(
        Long id,
        Long applicationId,
        String interviewerName,
        LocalDateTime scheduledAt,
        String meetingLink,
        InterviewType type,
        InterviewStatus status
) {
}
