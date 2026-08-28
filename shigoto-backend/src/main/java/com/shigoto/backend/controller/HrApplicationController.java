package com.shigoto.backend.controller;

import com.shigoto.backend.dto.HrApplicationDetailsDTO;
import com.shigoto.backend.dto.HomeTaskAssignmentRequestDTO;
import com.shigoto.backend.dto.HrApplicationStatusUpdateRequestDTO;
import com.shigoto.backend.dto.HrNotesUpdateRequestDTO;
import com.shigoto.backend.dto.HrCandidateFeedbackRequestDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
public class HrApplicationController {
    private final ApplicationService applicationService;
    private final AuthService authService;

    @GetMapping("/{applicationId}")
    public HrApplicationDetailsDTO getApplication(
            @PathVariable Long applicationId, Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.getHrApplicationDetails(applicationId, hr);
    }

    @GetMapping("/{applicationId}/cv")
    public ResponseEntity<Resource> downloadCv(
            @PathVariable Long applicationId, Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        ApplicationService.CvDownload download = applicationService.getHrApplicationCv(applicationId, hr);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.downloadFilename()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }

    @PutMapping("/{applicationId}/notes")
    public HrApplicationDetailsDTO updateNotes(
            @PathVariable Long applicationId,
            @RequestBody HrNotesUpdateRequestDTO request,
            Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("HR notes request is required");
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.updateHrNotes(applicationId, request.hrNotes(), hr);
    }

    @PutMapping("/{applicationId}/status")
    public HrApplicationDetailsDTO updateStatus(
            @PathVariable Long applicationId,
            @RequestBody HrApplicationStatusUpdateRequestDTO request,
            Authentication authentication) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Application status is required");
        }
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.transitionHrApplicationStatus(applicationId, request.status(), hr);
    }

    @PutMapping("/{applicationId}/reject")
    public HrApplicationDetailsDTO rejectApplication(
            @PathVariable Long applicationId,
            @RequestBody(required = false) HrCandidateFeedbackRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.rejectHrApplication(
                applicationId, request == null ? null : request.candidateFeedback(), hr);
    }

    @PutMapping("/{applicationId}/candidate-feedback")
    public HrApplicationDetailsDTO updateCandidateFeedback(
            @PathVariable Long applicationId,
            @RequestBody(required = false) HrCandidateFeedbackRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.updateCandidateFeedback(
                applicationId, request == null ? null : request.candidateFeedback(), hr);
    }

    @PostMapping("/{applicationId}/home-task")
    public HrApplicationDetailsDTO assignHomeTask(
            @PathVariable Long applicationId,
            @RequestBody HomeTaskAssignmentRequestDTO request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Home task request is required");
        }
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.assignHomeTask(
                applicationId, request.taskInstructions(), request.deadline(), hr);
    }
}
