package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;

public record AuthenticatedUserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role
) {
    public static AuthenticatedUserResponseDTO from(User user) {
        return new AuthenticatedUserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
