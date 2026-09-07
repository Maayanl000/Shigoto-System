package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.EmploymentType;

import java.time.LocalDateTime;

/**
 * Represents the complete company-scoped application view used by HR.
 */
public record HrApplicationDetailsDTO(
        Long applicationId,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        String coverLetter,
        String hrNotes,
        LocalDateTime taskDeadline,
        String taskInstructions,
        String taskRepoUrl,
        Long taskReviewerId,
        String taskReviewerName,
        String candidateFeedback,
        Long candidateId,
        String firstName,
        String lastName,
        String email,
        String githubProfileUrl,
        String currentTitle,
        String desiredRole,
        EmploymentType employmentType,
        boolean student,
        Long jobId,
        String jobTitle,
        String location,
        String companyName,
        boolean cvAvailable,
        GithubAnalysisDTO githubAnalysis,
        Long version
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param application the application being processed
     * @return a DTO populated from the supplied domain entity
     */
    public static HrApplicationDetailsDTO from(Application application) {
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new HrApplicationDetailsDTO(
                application.getId(), application.getStatus(), application.getAppliedAt(),
                application.getCoverLetter(), application.getHrNotes(), application.getTaskDeadline(),
                application.getTaskInstructions(), application.getTaskRepoUrl(),
                application.getTaskReviewer() == null ? null : application.getTaskReviewer().getId(),
                application.getTaskReviewer() == null ? null
                        : (application.getTaskReviewer().getFirstName() + " "
                        + application.getTaskReviewer().getLastName()).trim(),
                application.getCandidateFeedback(),
                candidate.getId(), candidate.getFirstName(),
                candidate.getLastName(), candidate.getEmail(), candidate.getGithubProfileUrl(),
                candidate.getCurrentTitle(), candidate.getDesiredRole(), candidate.getEmploymentType(),
                candidate.isStudent(), job.getId(), job.getTitle(), job.getLocation(),
                job.getCompany().getName(), application.getCvUrl() != null && !application.getCvUrl().isBlank(),
                GithubAnalysisDTO.from(candidate.getGithubData()), application.getVersion()
        );
    }
}
