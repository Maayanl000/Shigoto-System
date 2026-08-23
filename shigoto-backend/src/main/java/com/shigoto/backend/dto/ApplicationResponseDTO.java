package com.shigoto.backend.dto;

import com.shigoto.backend.entity.ApplicationStatus;
import java.time.LocalDateTime;

public record ApplicationResponseDTO(
        Long id,
        Long candidateId,
        Long jobId,
        String jobTitle,
        String companyName,
        String location,
        String coverLetter,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime taskDeadline,
        String taskRepoUrl
) {}
