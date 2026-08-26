package com.shigoto.backend.controller;

import com.shigoto.backend.dto.HrApplicationDetailsDTO;
import com.shigoto.backend.dto.HrNotesUpdateRequestDTO;
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
}
