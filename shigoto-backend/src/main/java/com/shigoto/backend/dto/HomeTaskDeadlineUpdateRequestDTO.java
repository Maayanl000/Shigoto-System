package com.shigoto.backend.dto;

import java.time.LocalDateTime;

/**
 * Defines a revised home-task deadline and expected application version submitted by HR.
 */
public record HomeTaskDeadlineUpdateRequestDTO(LocalDateTime deadline, Long version) {}
