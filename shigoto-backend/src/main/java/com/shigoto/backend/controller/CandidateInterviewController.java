package com.shigoto.backend.controller;

import com.shigoto.backend.dto.CandidateInterviewResponseDTO;
import com.shigoto.backend.service.InterviewService;
import com.shigoto.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes candidate interview HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CandidateInterviewController {

    private final InterviewService interviewService;
    private final AuthService authService;

    /**
     * Returns candidate-visible interviews for an application owned by the authenticated candidate.
     * @param applicationId the application identifier
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing candidate-visible interviews for the application
     */
    @GetMapping("/applications/{applicationId}/interviews")
    public ResponseEntity<List<CandidateInterviewResponseDTO>> getInterviewsByApplication(
            @PathVariable Long applicationId,
            Authentication authentication) {
        var candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(interviewService.getCandidateInterviews(applicationId, candidate));
    }

    /**
     * Returns interviews across all applications owned by the authenticated candidate.
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing all interviews visible to the candidate
     */
    @GetMapping("/interviews/mine")
    public ResponseEntity<List<CandidateInterviewResponseDTO>> getMyInterviews(Authentication authentication) {
        var candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(interviewService.getCandidateInterviews(candidate));
    }
}
