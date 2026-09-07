package com.shigoto.backend.controller;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.dto.HrApplicationSummaryDTO;
import com.shigoto.backend.dto.StaffApplicationResponseDTO;
import com.shigoto.backend.dto.TaskSubmissionRequestDTO;
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

/**
 * Exposes application HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;
    private final AuthService authService;

    /**
     * Accepts a multipart candidate application and returns the persisted submission.
     * @param jobId the job identifier
     * @param coverLetter the cover letter
     * @param cv the uploaded CV file
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing the newly created application
     */
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

    /**
     * Returns applications visible to the authenticated HR user, optionally filtered by job.
     * @param jobId the job identifier
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing the requested application summaries
     */
    @GetMapping
    public ResponseEntity<List<HrApplicationSummaryDTO>> getAllApplications(
            @RequestParam(required = false) Long jobId,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(applicationService.getAllApplications(hr, jobId));
    }

    /**
     * Returns every application owned by the authenticated candidate.
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing the candidate's applications
     */
    @GetMapping("/mine")
    public ResponseEntity<List<ApplicationResponseDTO>> getMyApplications(Authentication authentication) {
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(applicationService.getApplicationsForCandidate(candidate));
    }

    /**
     * Returns a candidate's applications that belong to the authenticated HR user's company.
     * @param candidateId the candidate identifier
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing company-scoped applications for the candidate
     */
    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<StaffApplicationResponseDTO>> getApplicationsByCandidate(
            @PathVariable Long candidateId,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(applicationService.getApplicationsByCandidate(candidateId, hr));
    }

    /**
     * Returns one application after verifying ownership by the authenticated candidate.
     * @param applicationId the application identifier
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing the candidate-owned application
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponseDTO> getApplicationById(
            @PathVariable Long applicationId,
            Authentication authentication) {
        User candidate = authService.getAuthenticatedCandidate(authentication);
        return ResponseEntity.ok(applicationService.getOwnedApplicationById(applicationId, candidate));
    }

    /**
     * Streams a stored CV as an attachment after enforcing candidate ownership or HR company scope.
     * @param applicationId the application identifier
     * @param authentication the current Spring Security authentication
     * @return an HTTP attachment response containing the stored CV resource
     */
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

    /**
     * Records the authenticated candidate's repository submission for an assigned home task.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing the application with its task submission
     */
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
                applicationService.submitTask(
                        applicationId, request.repositoryUrl(), request.version(), candidate));
    }

    /**
     * Deletes a company-scoped application using the client's expected version.
     * @param id the entity identifier
     * @param version the client-visible version used for optimistic locking
     * @param authentication the current Spring Security authentication
     * @return an empty successful HTTP response after deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id,
            @RequestParam Long version,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        applicationService.deleteApplication(id, version, hr);
        return ResponseEntity.noContent().build();
    }
}
