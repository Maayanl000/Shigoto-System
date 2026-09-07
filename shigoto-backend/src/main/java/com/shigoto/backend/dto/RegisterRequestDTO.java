package com.shigoto.backend.dto;

/**
 * Defines candidate identity, credentials, and GitHub profile data submitted during registration.
 */
public record RegisterRequestDTO(
        String firstName,
        String lastName,
        String email,
        String password,
        String githubProfileUrl
) {}
