package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;

import java.time.LocalDateTime;

public record InterviewerCandidateReviewDTO(
        Long applicationId, String candidateName, String email,
        String currentTitle, String desiredRole, String githubProfileUrl,
        String jobTitle, String companyName, ApplicationStatus status,
        String taskInstructions, LocalDateTime taskDeadline, String taskRepoUrl,
        String taskReviewNotes
) {
    public static InterviewerCandidateReviewDTO from(Application application) {
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new InterviewerCandidateReviewDTO(
                application.getId(), (candidate.getFirstName() + " " + candidate.getLastName()).trim(),
                candidate.getEmail(), candidate.getCurrentTitle(), candidate.getDesiredRole(),
                candidate.getGithubProfileUrl(), job.getTitle(), job.getCompany().getName(),
                application.getStatus(), application.getTaskInstructions(), application.getTaskDeadline(),
                application.getTaskRepoUrl(), application.getTaskReviewNotes());
    }
}
