package com.shigoto.backend.controller;

import com.shigoto.backend.dto.HrApplicationDetailsDTO;
import com.shigoto.backend.dto.HomeTaskAssignmentRequestDTO;
import com.shigoto.backend.dto.HomeTaskDeadlineUpdateRequestDTO;
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

/**
 * Exposes hr application HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
public class HrApplicationController {
    private final ApplicationService applicationService;
    private final AuthService authService;

    /**
     * Returns detailed application data within the authenticated HR user's company.
     * @param applicationId the application identifier
     * @param authentication the current Spring Security authentication
     * @return the HR-facing details for the requested application
     */
    @GetMapping("/{applicationId}")
    public HrApplicationDetailsDTO getApplication(
            @PathVariable Long applicationId, Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.getHrApplicationDetails(applicationId, hr);
    }

    /**
     * Streams a stored CV as an attachment after enforcing candidate ownership or HR company scope.
     * @param applicationId the application identifier
     * @param authentication the current Spring Security authentication
     * @return an HTTP attachment response containing the stored CV resource
     */
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

    /**
     * Updates internal notes on a company-scoped application or assigned interview.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the application containing the persisted internal HR notes
     */
    @PutMapping("/{applicationId}/notes")
    public HrApplicationDetailsDTO updateNotes(
            @PathVariable Long applicationId,
            @RequestBody HrNotesUpdateRequestDTO request,
            Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("HR notes request is required");
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.updateHrNotes(applicationId, request.hrNotes(), request.version(), hr);
    }

    /**
     * Transitions a company-scoped application to the requested workflow status.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the application after the requested workflow transition
     */
    @PutMapping("/{applicationId}/status")
    public HrApplicationDetailsDTO updateStatus(
            @PathVariable Long applicationId,
            @RequestBody HrApplicationStatusUpdateRequestDTO request,
            Authentication authentication) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Application status is required");
        }
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.transitionHrApplicationStatus(
                applicationId, request.status(), request.version(), hr);
    }

    /**
     * Rejects a company-scoped application with candidate-facing feedback.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the rejected application with candidate-facing feedback
     */
    @PutMapping("/{applicationId}/reject")
    public HrApplicationDetailsDTO rejectApplication(
            @PathVariable Long applicationId,
            @RequestBody(required = false) HrCandidateFeedbackRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.rejectHrApplication(
                applicationId, request == null ? null : request.candidateFeedback(),
                request == null ? null : request.version(), hr);
    }

    /**
     * Updates candidate-facing feedback on a company-scoped application.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the application containing the persisted candidate feedback
     */
    @PutMapping("/{applicationId}/candidate-feedback")
    public HrApplicationDetailsDTO updateCandidateFeedback(
            @PathVariable Long applicationId,
            @RequestBody(required = false) HrCandidateFeedbackRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.updateCandidateFeedback(
                applicationId, request == null ? null : request.candidateFeedback(),
                request == null ? null : request.version(), hr);
    }

    /**
     * Assigns home-task instructions, a deadline, and a reviewer to a company application.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the application containing the assigned task, deadline, and reviewer
     */
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
                applicationId, request.taskInstructions(), request.deadline(), request.reviewerId(),
                request.version(), hr);
    }

    /**
     * Changes the deadline of an existing home task using optimistic locking.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the application containing the revised home-task deadline
     */
    @PutMapping("/{applicationId}/home-task/deadline")
    public HrApplicationDetailsDTO updateHomeTaskDeadline(
            @PathVariable Long applicationId,
            @RequestBody HomeTaskDeadlineUpdateRequestDTO request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Home task deadline request is required");
        }
        User hr = authService.getAuthenticatedHr(authentication);
        return applicationService.updateHomeTaskDeadline(
                applicationId, request.deadline(), request.version(), hr);
    }
}
