package com.shigoto.backend.dto;

public record LoginRequestDTO(
        String email,
        String password
) {}
