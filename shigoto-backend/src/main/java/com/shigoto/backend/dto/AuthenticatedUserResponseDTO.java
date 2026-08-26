package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.entity.EmploymentType;

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
