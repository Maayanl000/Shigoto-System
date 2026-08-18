package com.shigoto.backend.dto;

import com.shigoto.backend.entity.ApplicationStatus;
import java.time.LocalDateTime;

public record ApplicationResponseDTO(
        Long id,
        Long candidateId,
        Long jobId,
        String coverLetter,
        ApplicationStatus status,
        LocalDateTime appliedAt
) {}