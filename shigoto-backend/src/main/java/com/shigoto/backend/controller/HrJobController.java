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

@RestController
@RequestMapping("/api/hr/jobs")
@RequiredArgsConstructor
public class HrJobController {

    private final JobService jobService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<List<HrJobResponseDTO>> getJobs(Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(jobService.getJobsForHr(hr));
    }

    @PostMapping
    public ResponseEntity<HrJobResponseDTO> createJob(
            @RequestBody HrJobCreateRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(jobService.createJobForHr(hr, request));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<HrJobResponseDTO> updateJob(
            @PathVariable Long jobId,
            @RequestBody HrJobUpdateRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return ResponseEntity.ok(jobService.updateJobForHr(hr, jobId, request));
    }
}
