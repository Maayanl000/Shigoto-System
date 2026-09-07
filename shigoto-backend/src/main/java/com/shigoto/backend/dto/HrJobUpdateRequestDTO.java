package com.shigoto.backend.dto;

import com.shigoto.backend.entity.JobStatus;

/**
 * Defines editable job fields, publication status, and the expected job version.
 */
public record HrJobUpdateRequestDTO(
        String title,
        String description,
        String location,
        JobStatus status,
        Long version
) {}
