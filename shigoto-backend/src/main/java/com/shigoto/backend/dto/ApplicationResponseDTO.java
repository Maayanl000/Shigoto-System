package com.shigoto.backend.dto;

import com.shigoto.backend.entity.ApplicationStatus;
import java.time.LocalDateTime;

/**
 * Represents candidate-visible application state, task details, and version information.
 */
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
        String taskInstructions,
        String taskRepoUrl,
        String candidateFeedback,
        boolean cvAvailable,
        Long version
) {}
