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

/**
 * Exposes hr interview HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrInterviewController {
    private final InterviewService interviewService;
    private final AuthService authService;

    /**
     * Returns interviewers belonging to the authenticated HR user's company.
     * @param authentication the current Spring Security authentication
     * @return interviewers available within the HR user's company
     */
    @GetMapping("/interviewers")
    public List<HrInterviewerOptionDTO> getInterviewers(Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.getCompanyInterviewers(hr);
    }

    /**
     * Returns scheduled interviews for a company-scoped application.
     * @param applicationId the application identifier
     * @param authentication the current Spring Security authentication
     * @return the HR-visible interviews scheduled for the application
     */
    @GetMapping("/applications/{applicationId}/interviews")
    public List<HrScheduledInterviewResponseDTO> getApplicationInterviews(
            @PathVariable Long applicationId, Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.getHrApplicationInterviews(applicationId, hr);
    }

    /**
     * Schedules an interview for a company-scoped application.
     * @param applicationId the application identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the newly scheduled interview returned to HR
     */
    @PostMapping("/applications/{applicationId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public HrScheduledInterviewResponseDTO scheduleInterview(
            @PathVariable Long applicationId,
            @RequestBody HrInterviewScheduleRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.scheduleInterview(applicationId, request, hr);
    }

    /**
     * Changes an interview's time, meeting link, or interviewer using optimistic locking.
     * @param interviewId the interview identifier
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return the interview returned to HR with its updated schedule
     */
    @PutMapping("/interviews/{interviewId}")
    public HrScheduledInterviewResponseDTO rescheduleInterview(
            @PathVariable Long interviewId,
            @RequestBody HrInterviewRescheduleRequestDTO request,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.rescheduleInterview(interviewId, request, hr);
    }

    /**
     * Cancels a company-scoped interview using the client's expected version.
     * @param interviewId the interview identifier
     * @param version the client-visible version used for optimistic locking
     * @param authentication the current Spring Security authentication
     * @return the cancelled interview returned to the client
     */
    @PutMapping("/interviews/{interviewId}/cancel")
    public HrScheduledInterviewResponseDTO cancelInterview(
            @PathVariable Long interviewId,
            @RequestParam Long version,
            Authentication authentication) {
        User hr = authService.getAuthenticatedHr(authentication);
        return interviewService.cancelInterview(interviewId, version, hr);
    }
}
