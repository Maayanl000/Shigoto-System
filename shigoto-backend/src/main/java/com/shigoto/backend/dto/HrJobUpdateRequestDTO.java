package com.shigoto.backend.dto;

import com.shigoto.backend.entity.JobStatus;

public record HrJobUpdateRequestDTO(
        String title,
        String description,
        String location,
        JobStatus status
) {}
