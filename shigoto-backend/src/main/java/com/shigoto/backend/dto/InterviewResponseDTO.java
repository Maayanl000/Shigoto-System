package com.shigoto.backend.dto;

import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import java.time.LocalDateTime;

public record InterviewResponseDTO(
        Long id,
        Long applicationId,
        String interviewerName, // הזרקנו רק את שם המראיין! לא את כל הישות שלו
        LocalDateTime scheduledAt,
        String meetingLink,
        String feedback,
        InterviewType type,
        InterviewStatus status
) {}