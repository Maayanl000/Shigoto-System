package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;

import java.time.LocalDateTime;

/**
 * Represents company-owned job details and optimistic-lock version information for HR.
 */
public record HrJobResponseDTO(
        Long id,
        String title,
        String description,
        String location,
        JobStatus status,
        LocalDateTime createdAt,
        Long version
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param job the job
     * @return a DTO populated from the supplied domain entity
     */
    public static HrJobResponseDTO from(Job job) {
        return new HrJobResponseDTO(
                job.getId(), job.getTitle(), job.getDescription(), job.getLocation(),
                job.getStatus(), job.getCreatedAt(), job.getVersion());
    }
}
