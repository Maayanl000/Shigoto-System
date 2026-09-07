package com.shigoto.backend.dto;

import java.time.LocalDateTime;

/**
 * Represents the timestamp, status, and safe message returned for an API error.
 */
public record ErrorResponseDTO(
        String message,
        int status,
        LocalDateTime timestamp
) {}
