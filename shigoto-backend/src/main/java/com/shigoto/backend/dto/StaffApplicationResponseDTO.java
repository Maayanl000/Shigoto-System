package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;

public record StaffApplicationResponseDTO(
        Long id,
        Long candidateId,
        Long jobId,
        ApplicationStatus status,
        String hrNotes
) {
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
