package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;

/**
 * Represents an open job exposed to unauthenticated and candidate clients.
 */
public record PublicJobResponseDTO(
        Long id,
        String title,
        String description,
        String location,
        String companyName,
        JobStatus status
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param job the job
     * @return a DTO populated from the supplied domain entity
     */
    public static PublicJobResponseDTO from(Job job) {
        return new PublicJobResponseDTO(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getLocation(),
                job.getCompany().getName(),
                job.getStatus()
        );
    }
}
