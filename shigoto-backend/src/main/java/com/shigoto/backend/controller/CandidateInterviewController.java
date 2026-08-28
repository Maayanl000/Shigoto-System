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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CandidateInterviewController {

    private final InterviewService interviewService;
    private final AuthService authService;

    @GetMapping("/applications/{applicationId}/interviews")
    public ResponseEntity<List<CandidateInterviewResponseDTO>> getInterviewsByApplication(
            @PathVariable Long applicationId,
            Authentication authentication) {
        var candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(interviewService.getCandidateInterviews(applicationId, candidate));
    }

    @GetMapping("/interviews/mine")
    public ResponseEntity<List<CandidateInterviewResponseDTO>> getMyInterviews(Authentication authentication) {
        var candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(interviewService.getCandidateInterviews(candidate));
    }
}
