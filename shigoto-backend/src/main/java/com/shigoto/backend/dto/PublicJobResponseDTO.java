package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;

public record PublicJobResponseDTO(
        Long id,
        String title,
        String description,
        String location,
        String companyName,
        JobStatus status
) {
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
