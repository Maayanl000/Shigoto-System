package com.shigoto.backend.dto;

/**
 * Defines the email and password submitted to establish an authenticated session.
 */
public record LoginRequestDTO(
        String email,
        String password
) {}
