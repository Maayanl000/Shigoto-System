package com.shigoto.backend.controller;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.dto.TaskSubmissionRequestDTO;
import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.service.ApplicationService;
import com.shigoto.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;
    private final AuthService authService;

    public record ApplicationRequest(Long jobId, String cvUrl, String coverLetter) {}
    public record UpdateApplicationRequest(ApplicationStatus status, String hrNotes) {}

    @PostMapping
    public ResponseEntity<Application> createApplication(
            @RequestBody ApplicationRequest request,
            Authentication authentication) {
        if (request.jobId() == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(applicationService.createApplication(
                candidate, request.jobId(), request.cvUrl(), request.coverLetter()));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponseDTO>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ApplicationResponseDTO>> getMyApplications(Authentication authentication) {
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(applicationService.getApplicationsForCandidate(candidate));
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<ApplicationResponseDTO>> getApplicationsByCandidate(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(applicationService.getApplicationsByCandidate(candidateId));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponseDTO> getApplicationById(
            @PathVariable Long applicationId,
            Authentication authentication) {
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(applicationService.getOwnedApplicationById(applicationId, candidate));
    }

    @PutMapping("/{applicationId}/task-submission")
    public ResponseEntity<ApplicationResponseDTO> submitTask(
            @PathVariable Long applicationId,
            @RequestBody TaskSubmissionRequestDTO request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Repository URL is required");
        }
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(
                applicationService.submitTask(applicationId, request.repositoryUrl(), candidate));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Application> updateApplication(
            @PathVariable Long id,
            @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(
                id, request.status(), request.hrNotes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}
