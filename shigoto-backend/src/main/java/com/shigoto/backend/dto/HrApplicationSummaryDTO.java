package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

public record HrApplicationSummaryDTO(
        Long applicationId,
        Long candidateId,
        String candidateName,
        Long jobId,
        String jobTitle,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime statusChangedAt,
        InterviewType activeInterviewType
) {
    public static HrApplicationSummaryDTO from(Application application, InterviewType activeInterviewType) {
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new HrApplicationSummaryDTO(
                application.getId(),
                candidate.getId(),
                candidate.getFirstName() + " " + candidate.getLastName(),
                job.getId(),
                job.getTitle(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getStatusChangedAt(),
                activeInterviewType
        );
    }
}
