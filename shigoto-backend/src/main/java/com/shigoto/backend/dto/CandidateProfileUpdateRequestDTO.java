package com.shigoto.backend.dto;

import com.shigoto.backend.entity.EmploymentType;

public record CandidateProfileUpdateRequestDTO(
        String firstName,
        String lastName,
        String githubProfileUrl,
        String currentTitle,
        String desiredRole,
        EmploymentType employmentType,
        boolean student
) {}
