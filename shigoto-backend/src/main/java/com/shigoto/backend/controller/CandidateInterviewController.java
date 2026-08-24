package com.shigoto.backend.controller;

import com.shigoto.backend.dto.CandidateInterviewResponseDTO;
import com.shigoto.backend.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class CandidateInterviewController {

    private final InterviewService interviewService;

    @GetMapping("/{applicationId}/interviews")
    public ResponseEntity<List<CandidateInterviewResponseDTO>> getInterviewsByApplication(
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewService.getCandidateInterviews(applicationId));
    }
}
