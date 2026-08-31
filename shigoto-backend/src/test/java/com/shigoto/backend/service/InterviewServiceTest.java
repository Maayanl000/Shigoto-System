package com.shigoto.backend.service;

import com.shigoto.backend.dto.HrInterviewScheduleRequestDTO;
import com.shigoto.backend.dto.HrInterviewRescheduleRequestDTO;
import com.shigoto.backend.dto.CandidateInterviewResponseDTO;
import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.NotificationType;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.InterviewSlotConflictException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.messaging.NotificationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class InterviewServiceTest {
    private InterviewRepository interviewRepository;
    private ApplicationRepository applicationRepository;
    private UserRepository userRepository;
    private NotificationEventPublisher notificationEventPublisher;
    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        interviewRepository = mock(InterviewRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        userRepository = mock(UserRepository.class);
        notificationEventPublisher = mock(NotificationEventPublisher.class);
        interviewService = new InterviewService(interviewRepository, applicationRepository, userRepository,
                notificationEventPublisher);
    }

    @Test
    void hrReceivesOnlyRepositoryScopedCompanyInterviewers() {
        Company company = company(1L, "Wix");
        User hr = hr(company);
        User interviewer = interviewer(5L, company);
        when(userRepository.findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(Role.INTERVIEWER, company))
                .thenReturn(List.of(interviewer));

        var result = interviewService.getCompanyInterviewers(hr);

        assertEquals(List.of(5L), result.stream().map(option -> option.interviewerId()).toList());
        assertEquals("Dana Levi", result.getFirst().fullName());
    }

    @Test
    void hrWithoutCompanyCannotListInterviewersOrSchedule() {
        User hr = User.builder().role(Role.HR).build();
        assertThrows(AccessDeniedException.class, () -> interviewService.getCompanyInterviewers(hr));
        assertThrows(AccessDeniedException.class, () ->
                interviewService.scheduleInterview(1L, validRequest(5L, InterviewType.TECHNICAL), hr));
        verify(userRepository, never()).findByRoleAndCompanyOrderByFirstNameAscLastNameAsc(any(), any());
    }

    @Test
    void ownCompanyTechnicalInterviewSchedulesAndUpdatesApplication() {
        Company company = company(1L, "Wix");
        User hr = hr(company);
        User interviewer = interviewer(5L, company);
        Application application = application(company, ApplicationStatus.TASK_APPROVED);
        application.setTaskInstructions("Build an API");
        application.setTaskDeadline(LocalDateTime.now().plusDays(1));
        application.setTaskRepoUrl("https://github.com/candidate/task");
        HrInterviewScheduleRequestDTO request = validRequest(5L, InterviewType.TECHNICAL);
        stubScopedEntities(company, application, interviewer, request);
        when(interviewRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Interview interview = invocation.getArgument(0);
            interview.setId(9L);
            return interview;
        });

        var response = interviewService.scheduleInterview(7L, request, hr);

        assertEquals(ApplicationStatus.TECH_INTERVIEW_SCHEDULED, application.getStatus());
        assertEquals(ApplicationStatus.TECH_INTERVIEW_SCHEDULED, response.applicationStatus());
        assertEquals(InterviewStatus.SCHEDULED, response.status());
        verify(applicationRepository).save(application);
        verify(interviewRepository).saveAndFlush(any(Interview.class));
    }

    @Test
    void hrInterviewCanScheduleTechnicalInterviewWithoutCreatingTaskData() {
        Company company = company(1L, "Wix");
        User hr = hr(company);
        User interviewer = interviewer(5L, company);
        Application application = application(company, ApplicationStatus.HR_INTERVIEW);
        HrInterviewScheduleRequestDTO request = validRequest(5L, InterviewType.TECHNICAL);
        stubScopedEntities(company, application, interviewer, request);
        when(interviewRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Interview interview = invocation.getArgument(0);
            interview.setId(9L);
            return interview;
        });

        var response = interviewService.scheduleInterview(7L, request, hr);

        assertEquals(ApplicationStatus.TECH_INTERVIEW_SCHEDULED, response.applicationStatus());
        assertEquals(null, application.getTaskInstructions());
        assertEquals(null, application.getTaskDeadline());
        assertEquals(null, application.getTaskRepoUrl());
        ArgumentCaptor<com.shigoto.backend.messaging.CandidateNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(com.shigoto.backend.messaging.CandidateNotificationEvent.class);
        verify(notificationEventPublisher).publishAfterCommit(eventCaptor.capture());
        assertEquals(NotificationType.INTERVIEW_SCHEDULED, eventCaptor.getValue().type());
    }

    @Test
    void technicalAndManagerInterviewSequencingRemainsControlled() {
        Company company = company(1L, "Wix");
        Application applied = application(company, ApplicationStatus.APPLIED);
        when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.of(applied));
        assertThrows(IllegalArgumentException.class, () -> interviewService.scheduleInterview(
                7L, validRequest(5L, InterviewType.TECHNICAL), hr(company)));

        Application hrInterview = application(company, ApplicationStatus.HR_INTERVIEW);
        when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.of(hrInterview));
        assertThrows(IllegalArgumentException.class, () -> interviewService.scheduleInterview(
                7L, validRequest(5L, InterviewType.MANAGER), hr(company)));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void crossCompanyApplicationIsNotFound() {
        Company company = company(1L, "Wix");
        when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> interviewService.scheduleInterview(
                7L, validRequest(5L, InterviewType.TECHNICAL), hr(company)));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void crossCompanyInterviewerIsNotFound() {
        Company company = company(1L, "Wix");
        Application application = application(company, ApplicationStatus.TASK_APPROVED);
        HrInterviewScheduleRequestDTO request = validRequest(50L, InterviewType.TECHNICAL);
        when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.of(application));
        when(userRepository.findByIdAndCompany(50L, company)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                interviewService.scheduleInterview(7L, request, hr(company)));
    }

    @Test
    void sameCompanyNonInterviewerIsRejected() {
        Company company = company(1L, "Wix");
        Application application = application(company, ApplicationStatus.TASK_APPROVED);
        User user = User.builder().id(5L).role(Role.HR).company(company).build();
        HrInterviewScheduleRequestDTO request = validRequest(5L, InterviewType.TECHNICAL);
        stubScopedEntities(company, application, user, request);
        assertThrows(IllegalArgumentException.class, () ->
                interviewService.scheduleInterview(7L, request, hr(company)));
    }

    @Test
    void pastDateAndInvalidMeetingUrlAreRejected() {
        Company company = company(1L, "Wix");
        HrInterviewScheduleRequestDTO past = new HrInterviewScheduleRequestDTO(
                5L, InterviewType.TECHNICAL, LocalDateTime.now().minusMinutes(1), "https://meet.example.com/1");
        assertThrows(IllegalArgumentException.class, () -> interviewService.scheduleInterview(7L, past, hr(company)));
        HrInterviewScheduleRequestDTO invalidLink = new HrInterviewScheduleRequestDTO(
                5L, InterviewType.TECHNICAL, LocalDateTime.now().plusDays(1), "javascript:alert(1)");
        assertThrows(IllegalArgumentException.class, () ->
                interviewService.scheduleInterview(7L, invalidLink, hr(company)));
    }

    @Test
    void terminalApplicationCannotBeScheduled() {
        Company company = company(1L, "Wix");
        for (ApplicationStatus status : List.of(
                ApplicationStatus.OFFER, ApplicationStatus.HIRED, ApplicationStatus.REJECTED)) {
            Application application = application(company, status);
            when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.of(application));
            assertThrows(IllegalArgumentException.class, () -> interviewService.scheduleInterview(
                    7L, validRequest(5L, InterviewType.TECHNICAL), hr(company)));
        }
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void duplicateAndInterviewerTimeConflictAreRejected() {
        Company company = company(1L, "Wix");
        Application application = application(company, ApplicationStatus.TASK_APPROVED);
        User interviewer = interviewer(5L, company);
        HrInterviewScheduleRequestDTO request = validRequest(5L, InterviewType.TECHNICAL);
        stubScopedEntities(company, application, interviewer, request);
        when(interviewRepository.existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNot(
                7L, 5L, request.scheduledAt(), InterviewStatus.CANCELED)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () ->
                interviewService.scheduleInterview(7L, request, hr(company)));

        when(interviewRepository.existsByApplicationIdAndInterviewerIdAndScheduledAtAndStatusNot(
                7L, 5L, request.scheduledAt(), InterviewStatus.CANCELED)).thenReturn(false);
        when(interviewRepository.existsByInterviewerIdAndScheduledAtAndStatusNot(
                5L, request.scheduledAt(), InterviewStatus.CANCELED)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () ->
                interviewService.scheduleInterview(7L, request, hr(company)));
    }

    @Test
    void ownCompanyHrReschedulesScheduledInterview() {
        Company company = company(1L, "Wix");
        User replacement = interviewer(6L, company);
        Interview interview = scheduledInterview(company, InterviewStatus.SCHEDULED);
        LocalDateTime newTime = LocalDateTime.now().plusDays(3);
        when(interviewRepository.findByIdAndApplicationJobCompany(9L, company)).thenReturn(Optional.of(interview));
        when(userRepository.findByIdAndCompany(6L, company)).thenReturn(Optional.of(replacement));
        when(interviewRepository.saveAndFlush(interview)).thenReturn(interview);

        var response = interviewService.rescheduleInterview(9L,
                new HrInterviewRescheduleRequestDTO(6L, newTime, " https://meet.example.com/new "), hr(company));

        assertEquals(6L, response.interviewerId());
        assertEquals(newTime, response.scheduledAt());
        assertEquals("https://meet.example.com/new", response.meetingLink());
        verify(interviewRepository).saveAndFlush(interview);
    }

    @Test
    void databaseSlotConflictIsMappedWithoutMaskingOtherIntegrityFailures() {
        Company company = company(1L, "Wix");
        User interviewer = interviewer(5L, company);
        Application application = application(company, ApplicationStatus.HR_INTERVIEW);
        HrInterviewScheduleRequestDTO request = validRequest(5L, InterviewType.TECHNICAL);
        stubScopedEntities(company, application, interviewer, request);
        PSQLException postgresFailure = mock(PSQLException.class);
        ServerErrorMessage serverError = mock(ServerErrorMessage.class);
        when(postgresFailure.getServerErrorMessage()).thenReturn(serverError);
        when(serverError.getConstraint()).thenReturn(InterviewService.ACTIVE_SLOT_INDEX);
        DataIntegrityViolationException slotFailure =
                new DataIntegrityViolationException("slot conflict", postgresFailure);
        when(interviewRepository.saveAndFlush(any(Interview.class))).thenThrow(slotFailure);

        InterviewSlotConflictException conflict = assertThrows(InterviewSlotConflictException.class,
                () -> interviewService.scheduleInterview(7L, request, hr(company)));

        assertEquals("Interviewer is no longer available at the selected time", conflict.getMessage());

        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        DataIntegrityViolationException unrelated = new DataIntegrityViolationException("other constraint");
        when(interviewRepository.saveAndFlush(any(Interview.class))).thenThrow(unrelated);
        assertEquals(unrelated, assertThrows(DataIntegrityViolationException.class,
                () -> interviewService.scheduleInterview(7L, request, hr(company))));
    }

    @Test
    void crossCompanyHrCannotRescheduleInterview() {
        Company company = company(1L, "Wix");
        when(interviewRepository.findByIdAndApplicationJobCompany(9L, company)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> interviewService.rescheduleInterview(
                9L, validReschedule(5L), hr(company)));
    }

    @Test
    void rescheduleRejectsWrongCompanyAndNonInterviewer() {
        Company company = company(1L, "Wix");
        Interview interview = scheduledInterview(company, InterviewStatus.SCHEDULED);
        when(interviewRepository.findByIdAndApplicationJobCompany(9L, company)).thenReturn(Optional.of(interview));
        when(userRepository.findByIdAndCompany(50L, company)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> interviewService.rescheduleInterview(
                9L, validReschedule(50L), hr(company)));

        User nonInterviewer = User.builder().id(6L).role(Role.HR).company(company).build();
        when(userRepository.findByIdAndCompany(6L, company)).thenReturn(Optional.of(nonInterviewer));
        assertThrows(IllegalArgumentException.class, () -> interviewService.rescheduleInterview(
                9L, validReschedule(6L), hr(company)));
    }

    @Test
    void rescheduleRejectsPastTimeAndInvalidMeetingUrl() {
        Company company = company(1L, "Wix");
        assertThrows(IllegalArgumentException.class, () -> interviewService.rescheduleInterview(9L,
                new HrInterviewRescheduleRequestDTO(5L, LocalDateTime.now().minusMinutes(1),
                        "https://meet.example.com/1"), hr(company)));
        assertThrows(IllegalArgumentException.class, () -> interviewService.rescheduleInterview(9L,
                new HrInterviewRescheduleRequestDTO(5L, LocalDateTime.now().plusDays(1),
                        "not-a-url"), hr(company)));
    }

    @Test
    void completedAndCanceledInterviewsCannotBeRescheduled() {
        Company company = company(1L, "Wix");
        for (InterviewStatus status : List.of(InterviewStatus.COMPLETED, InterviewStatus.CANCELED)) {
            Interview interview = scheduledInterview(company, status);
            when(interviewRepository.findByIdAndApplicationJobCompany(9L, company))
                    .thenReturn(Optional.of(interview));
            assertThrows(IllegalArgumentException.class, () -> interviewService.rescheduleInterview(
                    9L, validReschedule(5L), hr(company)));
        }
    }

    @Test
    void ownCompanyHrCancelsScheduledTechnicalInterviewAndPreservesHistory() {
        Company company = company(1L, "Wix");
        Interview interview = scheduledInterview(company, InterviewStatus.SCHEDULED);
        when(interviewRepository.findByIdAndApplicationJobCompany(9L, company)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);

        var response = interviewService.cancelInterview(9L, hr(company));

        assertEquals(InterviewStatus.CANCELED, response.status());
        assertEquals(ApplicationStatus.TASK_APPROVED, interview.getApplication().getStatus());
        verify(interviewRepository).save(interview);
        verify(interviewRepository, never()).delete(any());
    }

    @Test
    void cancelingDirectTechnicalInterviewRestoresHrInterviewStage() {
        Company company = company(1L, "Wix");
        Interview interview = scheduledInterview(company, InterviewStatus.SCHEDULED);
        interview.getApplication().setTaskInstructions(null);
        interview.getApplication().setTaskDeadline(null);
        interview.getApplication().setTaskRepoUrl(null);
        when(interviewRepository.findByIdAndApplicationJobCompany(9L, company)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);

        var response = interviewService.cancelInterview(9L, hr(company));

        assertEquals(InterviewStatus.CANCELED, response.status());
        assertEquals(ApplicationStatus.HR_INTERVIEW, response.applicationStatus());
    }

    @Test
    void repeatedAndCompletedCancelAreRejected() {
        Company company = company(1L, "Wix");
        for (InterviewStatus status : List.of(InterviewStatus.CANCELED, InterviewStatus.COMPLETED)) {
            Interview interview = scheduledInterview(company, status);
            when(interviewRepository.findByIdAndApplicationJobCompany(9L, company))
                    .thenReturn(Optional.of(interview));
            assertThrows(IllegalArgumentException.class, () -> interviewService.cancelInterview(9L, hr(company)));
        }
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void candidateReadsScheduledInterviewsWithoutFeedback() {
        Company company = company(1L, "Wix");
        User candidate = User.builder().id(3L).role(Role.CANDIDATE).build();
        Application application = application(company, ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        application.setCandidate(candidate);
        Interview interview = Interview.builder().id(9L).application(application).interviewer(interviewer(5L, company))
                .scheduledAt(LocalDateTime.now().plusDays(1)).meetingLink("https://meet.example.com/1")
                .feedback("private feedback").type(InterviewType.TECHNICAL).status(InterviewStatus.CANCELED).build();
        Interview completed = Interview.builder().id(10L).application(application).interviewer(interviewer(5L, company))
                .scheduledAt(LocalDateTime.now().minusDays(1)).meetingLink("https://meet.example.com/2")
                .feedback("more private feedback").type(InterviewType.HR).status(InterviewStatus.COMPLETED).build();
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(7L)).thenReturn(List.of(completed, interview));

        var response = interviewService.getCandidateInterviews(7L, candidate);

        assertEquals(2, response.size());
        assertEquals("Dana Levi", response.getFirst().interviewerName());
        assertEquals(InterviewStatus.COMPLETED, response.getFirst().status());
        assertEquals(InterviewStatus.CANCELED, response.get(1).status());
        assertFalse(List.of(response.getFirst().getClass().getRecordComponents()).stream()
                .anyMatch(component -> component.getName().equals("feedback")));
    }

    @Test
    void candidateAggregatesOnlyOwnInterviewsWithSafeJobContext() {
        Company company = company(1L, "Wix");
        User candidate = User.builder().id(3L).role(Role.CANDIDATE).build();
        Application application = Application.builder().id(7L).candidate(candidate)
                .job(Job.builder().title("Backend Engineer").company(company).build())
                .status(ApplicationStatus.TECH_INTERVIEW_SCHEDULED).build();
        Interview interview = Interview.builder().id(9L).application(application).interviewer(interviewer(5L, company))
                .scheduledAt(LocalDateTime.now().plusDays(1)).meetingLink("https://meet.example.com/1")
                .feedback("private feedback").type(InterviewType.TECHNICAL).status(InterviewStatus.SCHEDULED).build();
        when(interviewRepository.findByApplicationCandidateIdOrderByScheduledAtAsc(3L))
                .thenReturn(List.of(interview));

        var response = interviewService.getCandidateInterviews(candidate);

        assertEquals(1, response.size());
        assertEquals(7L, response.getFirst().applicationId());
        assertEquals("Backend Engineer", response.getFirst().jobTitle());
        assertEquals("Wix", response.getFirst().companyName());
        assertFalse(List.of(response.getFirst().getClass().getRecordComponents()).stream()
                .anyMatch(component -> component.getName().equals("feedback")));
        verify(interviewRepository).findByApplicationCandidateIdOrderByScheduledAtAsc(3L);
    }

    @Test
    void nonCandidateCannotAggregateCandidateInterviews() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> interviewService.getCandidateInterviews(hr(company(1L, "Wix"))));
        verify(interviewRepository, never()).findByApplicationCandidateIdOrderByScheduledAtAsc(any());
    }

    @Test
    void interviewerListsOnlyRepositoryScopedOwnInterviews() {
        Company company = company(1L, "Wix");
        User owner = interviewer(5L, company);
        Interview ownInterview = interviewerInterview(9L, owner, InterviewStatus.SCHEDULED);
        when(interviewRepository.findByInterviewerIdOrderByScheduledAtAsc(5L))
                .thenReturn(List.of(ownInterview));

        var response = interviewService.getInterviewerInterviews(owner);

        assertEquals(1, response.size());
        assertEquals(9L, response.getFirst().interviewId());
        assertEquals("Candidate Name", response.getFirst().candidateName());
        verify(interviewRepository).findByInterviewerIdOrderByScheduledAtAsc(5L);
    }

    @Test
    void ownedScheduledInterviewFeedbackPersistsAndCompletesOnlyInterview() {
        Company company = company(1L, "Wix");
        User owner = interviewer(5L, company);
        Interview interview = interviewerInterview(9L, owner, InterviewStatus.SCHEDULED);
        when(interviewRepository.findByIdAndInterviewerId(9L, 5L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);

        var response = interviewService.submitInterviewerFeedback(9L, "  Strong technical discussion.  ", owner);

        assertEquals("Strong technical discussion.", interview.getFeedback());
        assertEquals(InterviewStatus.COMPLETED, interview.getStatus());
        assertEquals(ApplicationStatus.TECH_INTERVIEW_SCHEDULED, interview.getApplication().getStatus());
        assertEquals(InterviewStatus.COMPLETED, response.status());
        verify(interviewRepository).save(interview);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void ownerUpdatesTrimmedPrivateNotesWithoutChangingStatuses() {
        Company company = company(1L, "Wix");
        User owner = interviewer(5L, company);
        Interview interview = interviewerInterview(9L, owner, InterviewStatus.SCHEDULED);
        when(interviewRepository.findByIdAndInterviewerId(9L, 5L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(interview)).thenReturn(interview);

        var response = interviewService.updateInterviewerNotes(9L, "  Ask about testing strategy.  ", owner);

        assertEquals("Ask about testing strategy.", response.interviewerNotes());
        assertEquals(InterviewStatus.SCHEDULED, interview.getStatus());
        assertEquals(ApplicationStatus.TECH_INTERVIEW_SCHEDULED, interview.getApplication().getStatus());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void otherInterviewerCannotReadOrUpdatePrivateNotes() {
        User other = interviewer(6L, company(1L, "Wix"));

        assertThrows(ResourceNotFoundException.class,
                () -> interviewService.updateInterviewerNotes(9L, "notes", other));
        verify(interviewRepository).findByIdAndInterviewerId(9L, 6L);
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void privateNotesValidationAndCandidatePrivacyAreEnforced() {
        User owner = interviewer(5L, company(1L, "Wix"));
        when(interviewRepository.findByIdAndInterviewerId(9L, 5L))
                .thenReturn(Optional.of(interviewerInterview(9L, owner, InterviewStatus.SCHEDULED)));
        assertThrows(IllegalArgumentException.class,
                () -> interviewService.updateInterviewerNotes(9L, "x".repeat(10001), owner));
        assertFalse(List.of(CandidateInterviewResponseDTO.class.getRecordComponents()).stream()
                .anyMatch(component -> component.getName().equals("interviewerNotes")));
    }

    @Test
    void candidateReviewRequiresAssignedInterviewOrSubmittedCompanyTask() {
        Company company = company(1L, "Wix");
        User owner = interviewer(5L, company);
        Application application = application(company, ApplicationStatus.HR_INTERVIEW);
        application.setCandidate(User.builder().id(3L).firstName("Candidate").lastName("Name")
                .email("candidate@example.com").role(Role.CANDIDATE).build());
        when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.of(application));
        when(interviewRepository.existsByApplicationIdAndInterviewerId(7L, 5L)).thenReturn(true);

        assertEquals(7L, interviewService.getInterviewerCandidateReview(7L, owner).applicationId());

        when(interviewRepository.existsByApplicationIdAndInterviewerId(7L, 5L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,
                () -> interviewService.getInterviewerCandidateReview(7L, owner));
    }

    @Test
    void otherInterviewersCannotSubmitFeedbackRegardlessOfCompany() {
        User sameCompany = interviewer(6L, company(1L, "Wix"));
        User otherCompany = interviewer(7L, company(2L, "Google"));

        assertThrows(ResourceNotFoundException.class,
                () -> interviewService.submitInterviewerFeedback(9L, "Feedback", sameCompany));
        assertThrows(ResourceNotFoundException.class,
                () -> interviewService.submitInterviewerFeedback(9L, "Feedback", otherCompany));
        verify(interviewRepository).findByIdAndInterviewerId(9L, 6L);
        verify(interviewRepository).findByIdAndInterviewerId(9L, 7L);
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void completedAndCanceledInterviewsRejectFeedback() {
        Company company = company(1L, "Wix");
        User owner = interviewer(5L, company);
        for (InterviewStatus status : List.of(InterviewStatus.COMPLETED, InterviewStatus.CANCELED)) {
            Interview interview = interviewerInterview(9L, owner, status);
            when(interviewRepository.findByIdAndInterviewerId(9L, 5L)).thenReturn(Optional.of(interview));
            assertThrows(IllegalArgumentException.class,
                    () -> interviewService.submitInterviewerFeedback(9L, "Feedback", owner));
        }
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void feedbackValidationAndInterviewerRoleAreEnforced() {
        User owner = interviewer(5L, company(1L, "Wix"));
        assertThrows(IllegalArgumentException.class,
                () -> interviewService.submitInterviewerFeedback(9L, "   ", owner));
        assertThrows(IllegalArgumentException.class,
                () -> interviewService.submitInterviewerFeedback(9L, "x".repeat(10001), owner));
        assertThrows(AccessDeniedException.class,
                () -> interviewService.getInterviewerInterviews(hr(company(1L, "Wix"))));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void hrReadsScheduledInterviewsWithInterviewerIdForRescheduling() {
        Company company = company(1L, "Wix");
        Application application = application(company, ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        Interview interview = Interview.builder().id(9L).application(application).interviewer(interviewer(5L, company))
                .scheduledAt(LocalDateTime.now().plusDays(1)).meetingLink("https://meet.example.com/1")
                .type(InterviewType.TECHNICAL).status(InterviewStatus.SCHEDULED).build();
        when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(7L)).thenReturn(List.of(interview));

        var response = interviewService.getHrApplicationInterviews(7L, hr(company));

        assertEquals(9L, response.getFirst().interviewId());
        assertEquals(5L, response.getFirst().interviewerId());
    }

    @Test
    void ownCompanyHrReadsCompletedFeedbackWhileOtherCompanyIsBlocked() {
        Company wix = company(1L, "Wix");
        Company google = company(2L, "Google");
        Application application = application(wix, ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        Interview interview = Interview.builder().id(9L).application(application).interviewer(interviewer(5L, wix))
                .scheduledAt(LocalDateTime.now().minusDays(1)).feedback("Recommended")
                .type(InterviewType.TECHNICAL).status(InterviewStatus.COMPLETED).build();
        when(applicationRepository.findByIdAndJobCompany(7L, wix)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationIdOrderByScheduledAtAsc(7L)).thenReturn(List.of(interview));

        assertEquals("Recommended", interviewService.getHrApplicationInterviews(7L, hr(wix)).getFirst().feedback());
        assertThrows(ResourceNotFoundException.class,
                () -> interviewService.getHrApplicationInterviews(7L, hr(google)));
    }

    private void stubScopedEntities(Company company, Application application, User interviewer,
                                    HrInterviewScheduleRequestDTO request) {
        when(applicationRepository.findByIdAndJobCompany(7L, company)).thenReturn(Optional.of(application));
        when(userRepository.findByIdAndCompany(request.interviewerId(), company)).thenReturn(Optional.of(interviewer));
    }

    private HrInterviewScheduleRequestDTO validRequest(Long interviewerId, InterviewType type) {
        return new HrInterviewScheduleRequestDTO(
                interviewerId, type, LocalDateTime.now().plusDays(2), "https://meet.example.com/interview");
    }

    private HrInterviewRescheduleRequestDTO validReschedule(Long interviewerId) {
        return new HrInterviewRescheduleRequestDTO(
                interviewerId, LocalDateTime.now().plusDays(2), "https://meet.example.com/rescheduled");
    }

    private Interview scheduledInterview(Company company, InterviewStatus status) {
        Application application = application(company, ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        application.setTaskInstructions("Build an API");
        application.setTaskDeadline(LocalDateTime.now().plusDays(1));
        application.setTaskRepoUrl("https://github.com/candidate/task");
        return Interview.builder().id(9L).application(application).interviewer(interviewer(5L, company))
                .scheduledAt(LocalDateTime.now().plusDays(1)).meetingLink("https://meet.example.com/original")
                .type(InterviewType.TECHNICAL).status(status).build();
    }

    private Interview interviewerInterview(Long id, User owner, InterviewStatus status) {
        Application application = Application.builder().id(7L)
                .candidate(User.builder().id(3L).firstName("Candidate").lastName("Name").role(Role.CANDIDATE).build())
                .job(Job.builder().id(2L).title("Backend Engineer").company(owner.getCompany()).build())
                .status(ApplicationStatus.TECH_INTERVIEW_SCHEDULED).build();
        return Interview.builder().id(id).application(application).interviewer(owner)
                .scheduledAt(LocalDateTime.now().plusDays(1)).meetingLink("https://meet.example.com/interview")
                .type(InterviewType.TECHNICAL).status(status).build();
    }

    private Application application(Company company, ApplicationStatus status) {
        return Application.builder().id(7L).candidate(User.builder().id(3L).role(Role.CANDIDATE).build())
                .job(Job.builder().company(company).build()).status(status).build();
    }

    private User interviewer(Long id, Company company) {
        return User.builder().id(id).firstName("Dana").lastName("Levi").email("dana@example.com")
                .role(Role.INTERVIEWER).company(company).build();
    }

    private User hr(Company company) {
        return User.builder().id(1L).role(Role.HR).company(company).build();
    }

    private Company company(Long id, String name) {
        return Company.builder().id(id).name(name).build();
    }
}
