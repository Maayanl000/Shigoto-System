package com.shigoto.backend.dto;

import java.time.LocalDateTime;

public record HomeTaskDeadlineUpdateRequestDTO(LocalDateTime deadline, Long version) {}
