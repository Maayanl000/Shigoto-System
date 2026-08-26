package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;

import java.time.LocalDateTime;

public record HrJobResponseDTO(
        Long id,
        String title,
        String description,
        String location,
        JobStatus status,
        LocalDateTime createdAt
) {
    public static HrJobResponseDTO from(Job job) {
        return new HrJobResponseDTO(
                job.getId(), job.getTitle(), job.getDescription(), job.getLocation(),
                job.getStatus(), job.getCreatedAt());
    }
}
