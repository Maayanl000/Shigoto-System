package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;

import java.time.LocalDateTime;

/**
 * Represents a submitted home task assigned to an interviewer for review.
 */
public record InterviewerSubmittedTaskDTO(
        Long applicationId,
        Long candidateId,
        String candidateName,
        Long jobId,
        String jobTitle,
        String taskInstructions,
        LocalDateTime taskDeadline,
        String taskRepoUrl,
        String taskReviewNotes,
        ApplicationStatus status,
        Long version
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param application the application being processed
     * @return a DTO populated from the supplied domain entity
     */
    public static InterviewerSubmittedTaskDTO from(Application application) {
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new InterviewerSubmittedTaskDTO(
                application.getId(), candidate.getId(),
                (candidate.getFirstName() + " " + candidate.getLastName()).trim(),
                job.getId(), job.getTitle(), application.getTaskInstructions(),
                application.getTaskDeadline(), application.getTaskRepoUrl(),
                application.getTaskReviewNotes(), application.getStatus(), application.getVersion());
    }
}
