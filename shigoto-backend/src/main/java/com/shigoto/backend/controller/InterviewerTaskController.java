package com.shigoto.backend.controller;

import com.shigoto.backend.dto.InterviewerSubmittedTaskDTO;
import com.shigoto.backend.dto.InterviewerTaskReviewRequestDTO;
import com.shigoto.backend.dto.InterviewerCandidateReviewDTO;
import com.shigoto.backend.dto.InterviewerTaskReviewNotesRequestDTO;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.service.ApplicationService;
import com.shigoto.backend.service.AuthService;
import com.shigoto.backend.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes interviewer task HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/interviewer")
@RequiredArgsConstructor
public class InterviewerTaskController {
    private final ApplicationService applicationService;
    private final AuthService authService;
    private final InterviewService interviewService;

    /**
     * Returns submitted home tasks assigned to the authenticated interviewer.
     * @param authentication the current Spring Security authentication
     * @return submitted home tasks assigned to the authenticated interviewer
     */
    @GetMapping("/tasks")
    public List<InterviewerSubmittedTaskDTO> getSubmittedTasks(Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return applicationService.getSubmittedTasksForInterviewer(interviewer);
    }

    /**
     * Returns candidate context for an application assigned to the authenticated interviewer.
     * @param applicationId the application identifier
     * @param authentication the current Spring Security authentication
     * @return the candidate and application context assigned to the interviewer
     */
    @GetMapping("/applications/{applicationId}")
    public InterviewerCandidateReviewDTO getCandidateReview(
            @PathVariable Long applicationId, Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return interviewService.getInterviewerCandidateReview(applicationId, interviewer);
    }

    /**
     * Records the assigned interviewer's home-task review decision.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the task submission with its persisted review decision
     */
    @PutMapping("/applications/{applicationId}/task-review")
    public InterviewerSubmittedTaskDTO reviewTask(
            @PathVariable Long applicationId,
            @RequestBody InterviewerTaskReviewRequestDTO request,
            Authentication authentication) {
        if (request == null || request.decision() == null) {
            throw new IllegalArgumentException("Task review decision is required");
        }
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return applicationService.reviewSubmittedTask(
                applicationId, request.decision(), request.version(), interviewer);
    }

    /**
     * Updates internal reviewer notes on an assigned home task.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the task submission containing the persisted reviewer notes
     */
    @PutMapping("/applications/{applicationId}/task-review-notes")
    public InterviewerSubmittedTaskDTO updateTaskReviewNotes(
            @PathVariable Long applicationId,
            @RequestBody InterviewerTaskReviewNotesRequestDTO request,
            Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return applicationService.updateTaskReviewNotes(
                applicationId, request == null ? null : request.taskReviewNotes(),
                request == null ? null : request.version(), interviewer);
    }
}
