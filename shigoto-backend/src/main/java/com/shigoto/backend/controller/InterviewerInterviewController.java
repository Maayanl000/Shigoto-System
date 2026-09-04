package com.shigoto.backend.controller;

import com.shigoto.backend.dto.InterviewerFeedbackRequestDTO;
import com.shigoto.backend.dto.InterviewerInterviewResponseDTO;
import com.shigoto.backend.dto.InterviewerNotesRequestDTO;
import com.shigoto.backend.entity.User;
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
@RequestMapping("/api/interviewer/interviews")
@RequiredArgsConstructor
public class InterviewerInterviewController {
    private final InterviewService interviewService;
    private final AuthService authService;

    @GetMapping
    public List<InterviewerInterviewResponseDTO> getInterviews(Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return interviewService.getInterviewerInterviews(interviewer);
    }

    @PutMapping("/{interviewId}/feedback")
    public InterviewerInterviewResponseDTO submitFeedback(
            @PathVariable Long interviewId,
            @RequestBody InterviewerFeedbackRequestDTO request,
            Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return interviewService.submitInterviewerFeedback(
                interviewId,
                request == null ? null : request.feedback(),
                request == null ? null : request.version(),
                interviewer);
    }

    @PutMapping("/{interviewId}/notes")
    public InterviewerInterviewResponseDTO updateNotes(
            @PathVariable Long interviewId,
            @RequestBody InterviewerNotesRequestDTO request,
            Authentication authentication) {
        User interviewer = authService.getAuthenticatedInterviewer(authentication);
        return interviewService.updateInterviewerNotes(
                interviewId,
                request == null ? null : request.interviewerNotes(),
                request == null ? null : request.version(),
                interviewer);
    }
}
