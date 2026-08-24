package com.shigoto.backend.dto;

public record RegisterRequestDTO(
        String firstName,
        String lastName,
        String email,
        String password
) {}
