package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.entity.EmploymentType;

/**
 * Represents the authenticated user identity, role, company, and candidate profile returned to the frontend.
 */
public record AuthenticatedUserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        String companyName,
        String githubProfileUrl,
        String currentTitle,
        String desiredRole,
        EmploymentType employmentType,
        boolean student
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param user the user to validate
     * @return a DTO populated from the supplied domain entity
     */
    public static AuthenticatedUserResponseDTO from(User user) {
        return new AuthenticatedUserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getCompany() == null ? null : user.getCompany().getName(),
                user.getGithubProfileUrl(),
                user.getCurrentTitle(),
                user.getDesiredRole(),
                user.getEmploymentType(),
                user.isStudent()
        );
    }
}
