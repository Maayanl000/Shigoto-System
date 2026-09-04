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

@RestController
@RequestMapping("/api/interviewer")
@RequiredArgsConstructor
public class InterviewerTaskController {
    private final ApplicationService applicationService;
    private final AuthService authService;
    private final InterviewService interviewService;

    @GetMapping("/tasks")
    public List<InterviewerSubmittedTaskDTO> getSubmittedTasks(Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return applicationService.getSubmittedTasksForInterviewer(interviewer);
    }

    @GetMapping("/applications/{applicationId}")
    public InterviewerCandidateReviewDTO getCandidateReview(
            @PathVariable Long applicationId, Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return interviewService.getInterviewerCandidateReview(applicationId, interviewer);
    }

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
