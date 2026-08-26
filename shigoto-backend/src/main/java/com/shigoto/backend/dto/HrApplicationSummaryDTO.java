package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;

import java.time.LocalDateTime;

public record HrApplicationSummaryDTO(
        Long applicationId,
        Long candidateId,
        String candidateName,
        Long jobId,
        String jobTitle,
        ApplicationStatus status,
        LocalDateTime appliedAt
) {
    public static HrApplicationSummaryDTO from(Application application) {
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new HrApplicationSummaryDTO(
                application.getId(),
                candidate.getId(),
                candidate.getFirstName() + " " + candidate.getLastName(),
                job.getId(),
                job.getTitle(),
                application.getStatus(),
                application.getAppliedAt()
        );
    }
}
