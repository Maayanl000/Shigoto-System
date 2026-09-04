package com.shigoto.backend.controller;

import com.shigoto.backend.dto.HrInterviewerOptionDTO;
import com.shigoto.backend.dto.HrInterviewScheduleRequestDTO;
import com.shigoto.backend.dto.HrInterviewRescheduleRequestDTO;
import com.shigoto.backend.dto.HrScheduledInterviewResponseDTO;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.service.AuthService;
import com.shigoto.backend.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrInterviewController {
    private final InterviewService interviewService;
    private final AuthService authService;

    @GetMapping("/interviewers")
    public List<HrInterviewerOptionDTO> getInterviewers(Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.getCompanyInterviewers(hr);
    }

    @GetMapping("/applications/{applicationId}/interviews")
    public List<HrScheduledInterviewResponseDTO> getApplicationInterviews(
            @PathVariable Long applicationId, Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.getHrApplicationInterviews(applicationId, hr);
    }

    @PostMapping("/applications/{applicationId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public HrScheduledInterviewResponseDTO scheduleInterview(
            @PathVariable Long applicationId,
            @RequestBody HrInterviewScheduleRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.scheduleInterview(applicationId, request, hr);
    }

    @PutMapping("/interviews/{interviewId}")
    public HrScheduledInterviewResponseDTO rescheduleInterview(
            @PathVariable Long interviewId,
            @RequestBody HrInterviewRescheduleRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.rescheduleInterview(interviewId, request, hr);
    }

    @PutMapping("/interviews/{interviewId}/cancel")
    public HrScheduledInterviewResponseDTO cancelInterview(
            @PathVariable Long interviewId,
            @RequestParam Long version,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.cancelInterview(interviewId, version, hr);
    }
}
