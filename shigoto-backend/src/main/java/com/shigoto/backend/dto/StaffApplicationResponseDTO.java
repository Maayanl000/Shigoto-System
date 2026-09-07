package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;

/**
 * Represents application information visible to company staff.
 */
public record StaffApplicationResponseDTO(
        Long id,
        Long candidateId,
        Long jobId,
        ApplicationStatus status,
        String hrNotes
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param application the application being processed
     * @return a DTO populated from the supplied domain entity
     */
    public static StaffApplicationResponseDTO from(Application application) {
        return new StaffApplicationResponseDTO(
                application.getId(),
                application.getCandidate().getId(),
                application.getJob().getId(),
                application.getStatus(),
                application.getHrNotes()
        );
    }
}
