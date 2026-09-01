package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.EmploymentType;

import java.time.LocalDateTime;

public record HrApplicationDetailsDTO(
        Long applicationId,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        String coverLetter,
        String hrNotes,
        LocalDateTime taskDeadline,
        String taskInstructions,
        String taskRepoUrl,
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
        GithubAnalysisDTO githubAnalysis
) {
    public static HrApplicationDetailsDTO from(Application application) {
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new HrApplicationDetailsDTO(
                application.getId(), application.getStatus(), application.getAppliedAt(),
                application.getCoverLetter(), application.getHrNotes(), application.getTaskDeadline(),
                application.getTaskInstructions(), application.getTaskRepoUrl(), application.getCandidateFeedback(),
                candidate.getId(), candidate.getFirstName(),
                candidate.getLastName(), candidate.getEmail(), candidate.getGithubProfileUrl(),
                candidate.getCurrentTitle(), candidate.getDesiredRole(), candidate.getEmploymentType(),
                candidate.isStudent(), job.getId(), job.getTitle(), job.getLocation(),
                job.getCompany().getName(), GithubAnalysisDTO.from(candidate.getGithubData())
        );
    }
}
