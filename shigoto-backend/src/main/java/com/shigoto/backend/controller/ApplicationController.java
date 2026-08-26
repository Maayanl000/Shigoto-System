package com.shigoto.backend.controller;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.dto.HrApplicationSummaryDTO;
import com.shigoto.backend.dto.StaffApplicationResponseDTO;
import com.shigoto.backend.dto.TaskSubmissionRequestDTO;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.service.ApplicationService;
import com.shigoto.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;
    private final AuthService authService;

    public record UpdateApplicationRequest(ApplicationStatus status, String hrNotes) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApplicationResponseDTO> createApplication(
            @RequestParam Long jobId,
            @RequestParam(defaultValue = "") String coverLetter,
            @RequestPart("cv") MultipartFile cv,
            Authentication authentication) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(applicationService.createApplication(
                candidate, jobId, coverLetter, cv));
    }

    @GetMapping
    public ResponseEntity<List<HrApplicationSummaryDTO>> getAllApplications(Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(applicationService.getAllApplications(hr));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ApplicationResponseDTO>> getMyApplications(Authentication authentication) {
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(applicationService.getApplicationsForCandidate(candidate));
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<StaffApplicationResponseDTO>> getApplicationsByCandidate(
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

    @GetMapping("/{applicationId}/cv")
    public ResponseEntity<Resource> downloadCv(
            @PathVariable Long applicationId,
            Authentication authentication) {
        User candidate = authService.getAuthenticatedCandidate(authentication);
        ApplicationService.CvDownload download = applicationService.getOwnedCv(applicationId, candidate);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.downloadFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
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
    public ResponseEntity<StaffApplicationResponseDTO> updateApplication(
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
