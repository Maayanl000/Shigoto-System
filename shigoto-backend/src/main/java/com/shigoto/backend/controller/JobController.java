package com.shigoto.backend.controller;

import com.shigoto.backend.dto.HrJobCreateRequestDTO;
import com.shigoto.backend.dto.HrJobResponseDTO;
import com.shigoto.backend.dto.PublicJobResponseDTO;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.service.AuthService;
import com.shigoto.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes job HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final AuthService authService;

    /**
     * Returns all jobs currently open to public candidates.
     * @return an HTTP response containing all currently open jobs
     */
    @GetMapping
    public ResponseEntity<List<PublicJobResponseDTO>> getAllJobs() {
        return ResponseEntity.ok(jobService.getOpenJobs());
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

}
