package com.shigoto.backend.dto;

import com.shigoto.backend.entity.EmploymentType;

/**
 * Defines editable candidate profile fields submitted by the authenticated candidate.
 */
public record CandidateProfileUpdateRequestDTO(
        String firstName,
        String lastName,
        String githubProfileUrl,
        String currentTitle,
        String desiredRole,
        EmploymentType employmentType,
        boolean student
) {}
