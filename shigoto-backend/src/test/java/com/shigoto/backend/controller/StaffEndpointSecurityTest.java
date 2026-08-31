package com.shigoto.backend.controller;

import com.shigoto.backend.config.SecurityConfig;
import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.dto.HrApplicationDetailsDTO;
import com.shigoto.backend.dto.HrInterviewerOptionDTO;
import com.shigoto.backend.dto.CandidateInterviewResponseDTO;
import com.shigoto.backend.dto.NotificationResponseDTO;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.CompanyRepository;
import com.shigoto.backend.service.ApplicationService;
import com.shigoto.backend.service.AuthService;
import com.shigoto.backend.service.InterviewService;
import com.shigoto.backend.service.JobService;
import com.shigoto.backend.service.NotificationService;
import com.shigoto.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = {
        ApplicationController.class,
        AuthController.class,
        UserController.class,
        JobController.class,
        HrJobController.class,
        HrApplicationController.class,
        HrInterviewController.class,
        InterviewerTaskController.class,
        InterviewerInterviewController.class,
        CandidateInterviewController.class,
        NotificationController.class
})
@Import({
        SecurityConfig.class,
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
class StaffEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JobService jobService;
    @MockitoBean
    private InterviewService interviewService;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private JobRepository jobRepository;
    @MockitoBean
    private CompanyRepository companyRepository;

    @Test
    void anonymousCannotListApplications() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/applications/mine"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/interviewer/interviews"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/interviewer/interviews/9/feedback")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"private\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/interviewer/applications/1/task-review-notes")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskReviewNotes\":\"private\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotUseStaffApplicationEndpoints() throws Exception {
        mockMvc.perform(get("/api/applications").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/applications").param("jobId", "3")
                        .with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/applications/candidate/2").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/applications/1")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"hrNotes\":\"private\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/applications/1")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hr/applications/1").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hr/applications/1/cv").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanReadSafeApplicationDetails() throws Exception {
        User hr = User.builder().id(1L).email("hr@example.com").role(Role.HR)
                .company(com.shigoto.backend.entity.Company.builder().id(1L).name("Shigoto").build()).build();
        HrApplicationDetailsDTO response = new HrApplicationDetailsDTO(
                7L, ApplicationStatus.APPLIED, null, "Cover", "Notes", null, null, null, null,
                2L, "Dana", "Cohen", "dana@example.com", "https://github.com/dana",
                "Developer", "Backend Engineer", null, false, 3L, "Backend Engineer", "Remote", "Shigoto");
        when(authService.getAuthenticatedHr(any())).thenReturn(hr);
        when(applicationService.getHrApplicationDetails(7L, hr)).thenReturn(response);

        mockMvc.perform(get("/api/hr/applications/7").with(user("hr").roles("HR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(7))
                .andExpect(jsonPath("$.candidateId").value(2))
                .andExpect(jsonPath("$.jobTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.cvUrl").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.candidate").doesNotExist())
                .andExpect(jsonPath("$.job").doesNotExist());
    }

    @Test
    void hrApplicationBrowserPreflightIsPermitted() throws Exception {
        mockMvc.perform(options("/api/hr/applications/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void hrCanListApplications() throws Exception {
        User hr = User.builder().id(1L).email("hr@example.com").role(Role.HR)
                .company(com.shigoto.backend.entity.Company.builder().id(1L).name("Shigoto").build()).build();
        when(authService.getAuthenticatedHr(any())).thenReturn(hr);
        when(applicationService.getAllApplications(hr, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/applications").with(user("hr").roles("HR")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(applicationService).getAllApplications(hr, null);
    }

    @Test
    void hrCanFilterApplicationsByJobId() throws Exception {
        User hr = User.builder().id(1L).email("hr@example.com").role(Role.HR)
                .company(com.shigoto.backend.entity.Company.builder().id(1L).name("Shigoto").build()).build();
        when(authService.getAuthenticatedHr(any())).thenReturn(hr);
        when(applicationService.getAllApplications(hr, 3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/applications").param("jobId", "3")
                        .with(user("hr").roles("HR")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(applicationService).getAllApplications(hr, 3L);
    }

    @Test
    void invalidApplicationJobIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/applications").param("jobId", "not-a-number")
                        .with(user("hr").roles("HR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void candidateCannotUseOtherStaffEndpoints() throws Exception {
        mockMvc.perform(get("/api/users").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/jobs")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hr/jobs").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/hr/jobs")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/hr/jobs/1")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hr/interviewers")
                        .with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/hr/applications/1/interviews")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/hr/interviews/9")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/hr/interviews/9/cancel")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/interviewer/tasks")
                        .with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/interviewer/interviews")
                        .with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/interviewer/interviews/9/feedback")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"feedback\":\"private\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/interviewer/applications/1/task-review")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/interviewer/applications/1/task-review-notes")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"taskReviewNotes\":\"private\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCannotUseInterviewerTaskReviewEndpoints() throws Exception {
        mockMvc.perform(get("/api/interviewer/tasks").with(user("hr").roles("HR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/interviewer/applications/1/task-review")
                        .with(user("hr").roles("HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/interviewer/applications/1/task-review-notes")
                        .with(user("hr").roles("HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"taskReviewNotes\":\"private\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/interviewer/interviews").with(user("hr").roles("HR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/interviewer/interviews/9/feedback")
                        .with(user("hr").roles("HR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"feedback\":\"private\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanListOnlyServiceScopedInterviewerOptions() throws Exception {
        var company = com.shigoto.backend.entity.Company.builder().id(1L).name("Wix").build();
        User hr = User.builder().id(1L).role(Role.HR).company(company).build();
        when(authService.getAuthenticatedHr(any())).thenReturn(hr);
        when(interviewService.getCompanyInterviewers(hr)).thenReturn(List.of(
                new HrInterviewerOptionDTO(4L, "Dana Levi", "dana@wix.com")));

        mockMvc.perform(get("/api/hr/interviewers").with(user("hr").roles("HR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interviewerId").value(4))
                .andExpect(jsonPath("$[0].fullName").value("Dana Levi"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].company").doesNotExist());
    }

    @Test
    void hrJobCreateIgnoresSuppliedCompanyAndUsesAuthenticatedHr() throws Exception {
        var company = com.shigoto.backend.entity.Company.builder().id(1L).name("Wix").build();
        User hr = User.builder().id(1L).role(Role.HR).company(company).build();
        when(authService.getAuthenticatedHr(any())).thenReturn(hr);

        mockMvc.perform(post("/api/hr/jobs")
                        .with(user("hr").roles("HR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Developer",
                                  "description": "Build APIs",
                                  "location": "Remote",
                                  "company": {"id": 10, "name": "Google"}
                                }
                                """))
                .andExpect(status().isOk());

        verify(jobService).createJobForHr(
                org.mockito.ArgumentMatchers.same(hr),
                org.mockito.ArgumentMatchers.argThat(request ->
                        "Developer".equals(request.title())
                                && "Build APIs".equals(request.description())
                                && "Remote".equals(request.location())));
    }

    @Test
    void hrJobsCorsPreflightIsNotBlockedByAuthentication() throws Exception {
        mockMvc.perform(options("/api/hr/jobs")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void authRestorationCorsPreflightIsNotBlockedByAuthentication() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void notificationCorsPreflightIsNotBlockedByAuthentication() throws Exception {
        for (var request : List.of(
                options("/api/notifications/mine")
                        .header("Access-Control-Request-Method", "GET"),
                options("/api/notifications/42/read")
                        .header("Access-Control-Request-Method", "PUT"))) {
            mockMvc.perform(request
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Headers", "X-XSRF-TOKEN"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().string("Access-Control-Allow-Credentials", "true"));
        }
    }

    @Test
    void notificationEndpointsRemainCandidateOnly() throws Exception {
        mockMvc.perform(get("/api/notifications/mine"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/mine").with(user("hr").roles("HR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/notifications/42/read")
                        .with(user("interviewer").roles("INTERVIEWER")).with(csrf()))
                .andExpect(status().isForbidden());

        User candidate = User.builder().id(2L).email("candidate@example.com").role(Role.CANDIDATE).build();
        NotificationResponseDTO response = new NotificationResponseDTO(
                42L, com.shigoto.backend.entity.NotificationType.HOME_TASK_ASSIGNED,
                "New home task", "A home task was assigned.", 7L, null, LocalDateTime.now(), false);
        when(authService.getAuthenticatedCandidate(any())).thenReturn(candidate);
        when(notificationService.mine(candidate)).thenReturn(List.of(response));
        when(notificationService.markRead(42L, candidate)).thenReturn(new NotificationResponseDTO(
                response.notificationId(), response.type(), response.title(), response.message(),
                response.applicationId(), response.interviewId(), response.createdAt(), true));

        mockMvc.perform(get("/api/notifications/mine").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(42));
        mockMvc.perform(put("/api/notifications/42/read")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        verify(notificationService).mine(candidate);
        verify(notificationService).markRead(42L, candidate);
    }

    @Test
    void hrStatusChangeUsesAuthenticatedHrAndSafeResponse() throws Exception {
        User hr = User.builder().id(1L).role(Role.HR)
                .company(com.shigoto.backend.entity.Company.builder().id(1L).name("Wix").build()).build();
        HrApplicationDetailsDTO response = new HrApplicationDetailsDTO(
                1L, ApplicationStatus.REJECTED, null, "Cover", "Notes", null, null, null, null,
                2L, "Dana", "Cohen", "dana@example.com", null, null, null, null, false,
                3L, "Developer", "Remote", "Wix");
        when(authService.getAuthenticatedHr(any())).thenReturn(hr);
        when(applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.REJECTED, hr))
                .thenReturn(response);

        mockMvc.perform(put("/api/hr/applications/1/status")
                        .with(user("hr").roles("HR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.cvUrl").doesNotExist())
                .andExpect(jsonPath("$.candidate").doesNotExist())
                .andExpect(jsonPath("$.job").doesNotExist());
    }

    @Test
    void candidateCannotChangeHrStatusOrAssignHomeTask() throws Exception {
        mockMvc.perform(put("/api/hr/applications/1/status")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIRED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/hr/applications/1/status")
                        .with(user("interviewer").roles("INTERVIEWER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIRED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/hr/applications/1/home-task")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"deadline\":\"2026-12-01T12:00:00\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/hr/applications/1/home-task/deadline")
                        .with(user("candidate").roles("CANDIDATE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"deadline\":\"2026-12-01T12:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCanReadMineButCannotReadAnotherCandidatesApplication() throws Exception {
        User candidate = User.builder().id(2L).email("candidate@example.com").role(Role.CANDIDATE).build();
        when(authService.getAuthenticatedCandidate(any())).thenReturn(candidate);
        when(applicationService.getApplicationsForCandidate(candidate)).thenReturn(List.of());
        when(applicationService.getOwnedApplicationById(9L, candidate))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("not owned"));

        mockMvc.perform(get("/api/applications/mine").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(get("/api/applications/9").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void onlyCandidateCanReadOwnAggregatedInterviews() throws Exception {
        User candidate = User.builder().id(2L).email("candidate@example.com").role(Role.CANDIDATE).build();
        CandidateInterviewResponseDTO response = new CandidateInterviewResponseDTO(
                4L, 7L, "Backend Engineer", "Wix", "Dana Levi",
                LocalDateTime.of(2026, 9, 1, 10, 30), "https://meet.example.com/1",
                InterviewType.TECHNICAL, InterviewStatus.SCHEDULED);
        when(authService.getAuthenticatedCandidate(any())).thenReturn(candidate);
        when(interviewService.getCandidateInterviews(candidate)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/interviews/mine").with(user("candidate").roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(7))
                .andExpect(jsonPath("$[0].jobTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$[0].companyName").value("Wix"))
                .andExpect(jsonPath("$[0].feedback").doesNotExist());
        mockMvc.perform(get("/api/interviews/mine").with(user("hr").roles("HR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/interviews/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateStateChangeRequiresCsrfAndSucceedsWithToken() throws Exception {
        User candidate = User.builder().id(2L).email("candidate@example.com").role(Role.CANDIDATE).build();
        ApplicationResponseDTO response = new ApplicationResponseDTO(
                1L, 2L, 3L, "Developer", "Shigoto", "Remote", "Cover",
                ApplicationStatus.TASK_SUBMITTED, null, null, "Build a REST API", "https://github.com/user/repo", null);
        when(authService.getAuthenticatedCandidate(any())).thenReturn(candidate);
        when(applicationService.submitTask(1L, "https://github.com/user/repo", candidate)).thenReturn(response);

        var request = put("/api/applications/1/task-submission")
                .with(user("candidate").roles("CANDIDATE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repositoryUrl\":\"https://github.com/user/repo\"}");
        mockMvc.perform(request).andExpect(status().isForbidden());

        mockMvc.perform(put("/api/applications/1/task-submission")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repositoryUrl\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TASK_SUBMITTED"));
    }

    @Test
    void invalidEmploymentTypeIsRejectedByEnumBinding() throws Exception {
        mockMvc.perform(put("/api/auth/me/profile")
                        .with(user("candidate").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Dana",
                                  "lastName": "Cohen",
                                  "githubProfileUrl": "https://github.com/dana",
                                  "currentTitle": "Developer",
                                  "desiredRole": "Backend Developer",
                                  "employmentType": "CONTRACT",
                                  "student": false
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
