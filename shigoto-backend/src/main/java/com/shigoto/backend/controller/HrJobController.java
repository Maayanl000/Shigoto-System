package com.shigoto.backend.controller;

import com.shigoto.backend.dto.HrJobCreateRequestDTO;
import com.shigoto.backend.dto.HrJobResponseDTO;
import com.shigoto.backend.dto.HrJobUpdateRequestDTO;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.service.AuthService;
import com.shigoto.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes hr job HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/hr/jobs")
@RequiredArgsConstructor
public class HrJobController {

    private final JobService jobService;
    private final AuthService authService;

    /**
     * Returns jobs owned by the authenticated HR user's company.
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing jobs owned by the HR user's company
     */
    @GetMapping
    public ResponseEntity<List<HrJobResponseDTO>> getJobs(Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(jobService.getJobsForHr(hr));
    }

    /**
     * Creates a job owned by the authenticated HR user's company.
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing the newly created job
     */
    @PostMapping
    public ResponseEntity<HrJobResponseDTO> createJob(
            @RequestBody HrJobCreateRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(jobService.createJobForHr(hr, request));
    }

    /**
     * Updates a company-owned job using the version supplied in the request.
     * @param jobId the job identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return an HTTP response containing the updated job
     */
    @PutMapping("/{jobId}")
    public ResponseEntity<HrJobResponseDTO> updateJob(
            @PathVariable Long jobId,
            @RequestBody HrJobUpdateRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(jobService.updateJobForHr(hr, jobId, request));
    }
}
