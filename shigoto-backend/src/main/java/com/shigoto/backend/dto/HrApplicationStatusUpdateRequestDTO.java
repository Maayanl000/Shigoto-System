package com.shigoto.backend.dto;

import com.shigoto.backend.entity.ApplicationStatus;

/**
 * Defines an HR-requested application status transition and expected version.
 */
public record HrApplicationStatusUpdateRequestDTO(ApplicationStatus status, Long version) {}
