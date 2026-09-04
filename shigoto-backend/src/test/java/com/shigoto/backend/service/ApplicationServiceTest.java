package com.shigoto.backend.service;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.entity.TaskReviewDecision;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;
import com.shigoto.backend.entity.NotificationType;
import com.shigoto.backend.dto.InterviewerSubmittedTaskDTO;
import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.dto.HrApplicationDetailsDTO;
import com.shigoto.backend.dto.HrApplicationSummaryDTO;
import com.shigoto.backend.exception.DuplicateApplicationException;
import com.shigoto.backend.exception.ApplicationDeleteConflictException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.repository.InterviewRepository;
import com.shigoto.backend.messaging.NotificationEventPublisher;
import com.shigoto.backend.messaging.GithubAnalysisEventPublisher;
import com.shigoto.backend.repository.GithubDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest {

    private ApplicationRepository applicationRepository;
    private ApplicationService applicationService;
    private CvStorageService cvStorageService;
    private NotificationEventPublisher notificationEventPublisher;
    private InterviewRepository interviewRepository;
    private UserRepository userRepository;
    private GithubAnalysisEventPublisher githubAnalysisEventPublisher;
    private GithubDataRepository githubDataRepository;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        cvStorageService = mock(CvStorageService.class);
        notificationEventPublisher = mock(NotificationEventPublisher.class);
        interviewRepository = mock(InterviewRepository.class);
        userRepository = mock(UserRepository.class);
        githubAnalysisEventPublisher = mock(GithubAnalysisEventPublisher.class);
        githubDataRepository = mock(GithubDataRepository.class);
        applicationService = new ApplicationService(
                applicationRepository, userRepository, mock(JobRepository.class), cvStorageService,
                notificationEventPublisher, interviewRepository, githubAnalysisEventPublisher,
                githubDataRepository);
    }

    @Test
    void submitsTaskAndAdvancesStatus() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var response = applicationService.submitTask(
                1L, " https://github.com/example/home-task ", 0L, application.getCandidate());

        assertEquals(ApplicationStatus.TASK_SUBMITTED, response.status());
        assertEquals("Implement the documented API", response.taskInstructions());
        assertEquals("https://github.com/example/home-task", response.taskRepoUrl());
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void rejectsMissingApplication() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.submitTask(99L, "https://github.com/example/repo", 0L, candidate(3L)));
    }

    @Test
    void rejectsApplicationThatHasNotReachedTaskStage() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setStatus(ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(
                        1L, "https://github.com/example/repo", 0L, application.getCandidate()));
        verify(applicationRepository, never()).saveAndFlush(application);
    }

    @Test
    void rejectsSecondSubmission() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(
                        1L, "https://github.com/example/repo", 0L, application.getCandidate()));
    }

    @Test
    void rejectsMissingOrExpiredDeadline() {
        Application application = assignedApplication(null);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(
                1L, "https://github.com/example/repo", 0L, application.getCandidate()));

        application.setTaskDeadline(LocalDateTime.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(
                1L, "https://github.com/example/repo", 0L, application.getCandidate()));
    }

    @Test
    void rejectsMissingMalformedNonGithubAndProfileUrls() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        User candidate = application.getCandidate();
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, null, 0L, candidate));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, " ", 0L, candidate));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, "not a url", 0L, candidate));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://example.com/user/repo", 0L, candidate));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://github.com/user", 0L, candidate));
        String oversized = "https://github.com/user/" + "a".repeat(
                256 - "https://github.com/user/".length());
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, oversized, 0L, candidate));
    }

    @Test
    void repositoryUrlAtDatabaseMaximumIsAccepted() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        String maximum = "https://github.com/user/" + "a".repeat(
                255 - "https://github.com/user/".length());

        var response = applicationService.submitTask(1L, maximum, 0L, application.getCandidate());

        assertEquals(255, response.taskRepoUrl().length());
    }

    @Test
    void rejectsReadingAndSubmittingAnotherCandidatesApplication() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        User otherCandidate = candidate(4L);

        assertThrows(AccessDeniedException.class,
                () -> applicationService.getOwnedApplicationById(1L, otherCandidate));
        assertThrows(AccessDeniedException.class,
                () -> applicationService.submitTask(
                        1L, "https://github.com/example/repo", 0L, otherCandidate));
        verify(applicationRepository, never()).saveAndFlush(application);
    }

    @Test
    void createsApplicationForAuthenticatedCandidateAndPreservesDuplicateProtection() {
        User candidate = candidate(3L);
        candidate.setGithubProfileUrl("https://github.com/octocat");
        Job job = Job.builder().id(2L).title("Developer").location("Remote")
                .company(Company.builder().name("Example Company").build())
                .status(JobStatus.OPEN).build();
        JobRepository jobRepository = mock(JobRepository.class);
        applicationService = new ApplicationService(
                applicationRepository, mock(UserRepository.class), jobRepository, cvStorageService,
                notificationEventPublisher, interviewRepository, githubAnalysisEventPublisher,
                githubDataRepository);
        when(jobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByCandidateIdAndJobId(3L, 2L)).thenReturn(false);
        when(cvStorageService.store(any())).thenReturn("123e4567-e89b-12d3-a456-426614174000.pdf");
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(invocation -> {
                    Application saved = invocation.getArgument(0);
                    saved.setId(7L);
                    return saved;
                });
        MockMultipartFile cv = validCv();

        var created = applicationService.createApplication(candidate, 2L, "Cover note", cv);

        assertEquals(candidate.getId(), created.candidateId());
        assertEquals("Cover note", created.coverLetter());
        verify(applicationRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(application ->
                "123e4567-e89b-12d3-a456-426614174000.pdf".equals(application.getCvUrl())));
        verify(notificationEventPublisher).publishAfterCommit(argThat(event ->
                event.type() == NotificationType.APPLICATION_SUBMITTED
                        && event.candidateUserId().equals(3L)
                        && event.applicationId().equals(7L)
                        && event.interviewId() == null));
        verify(githubAnalysisEventPublisher).publishAfterCommit(argThat(event ->
                event.candidateUserId().equals(3L) && event.applicationId().equals(7L)
                        && event.githubUsername().equals("octocat")));
        verify(githubDataRepository).save(argThat(data -> data.getCandidate() == candidate
                && data.getStatus() == com.shigoto.backend.entity.GithubAnalysisStatus.PENDING));

        when(applicationRepository.existsByCandidateIdAndJobId(3L, 2L)).thenReturn(true);
        assertThrows(DuplicateApplicationException.class,
                () -> applicationService.createApplication(candidate, 2L, "Cover note", cv));
        verify(notificationEventPublisher, org.mockito.Mockito.times(1)).publishAfterCommit(any());
    }

    @Test
    void deletesStoredCvWhenApplicationDatabaseSaveFails() {
        User candidate = candidate(3L);
        Job job = Job.builder().id(2L).status(JobStatus.OPEN).build();
        JobRepository jobRepository = mock(JobRepository.class);
        applicationService = new ApplicationService(
                applicationRepository, mock(UserRepository.class), jobRepository, cvStorageService,
                notificationEventPublisher, interviewRepository, githubAnalysisEventPublisher,
                githubDataRepository);
        String storageKey = "123e4567-e89b-12d3-a456-426614174000.pdf";
        when(jobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(cvStorageService.store(any())).thenReturn(storageKey);
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(applicationRepository.existsByCandidateIdAndJobId(3L, 2L)).thenReturn(false, true);

        assertThrows(DuplicateApplicationException.class,
                () -> applicationService.createApplication(candidate, 2L, "Cover", validCv()));

        verify(cvStorageService).delete(storageKey);
        verify(notificationEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void candidateCanDownloadOwnCvButNotAnotherCandidatesCv() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setCvUrl("123e4567-e89b-12d3-a456-426614174000.pdf");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(cvStorageService.load(application.getCvUrl()))
                .thenReturn(new ByteArrayResource("%PDF-test".getBytes()));

        var download = applicationService.getOwnedCv(1L, application.getCandidate());

        assertEquals("cv-application-1.pdf", download.downloadFilename());
        assertThrows(AccessDeniedException.class,
                () -> applicationService.getOwnedCv(1L, candidate(4L)));
    }

    @Test
    void sameCompanyHrDeletesApplicationAndStoredCv() {
        Company company = Company.builder().id(10L).name("Example Company").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setCvUrl("123e4567-e89b-12d3-a456-426614174000.pdf");
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(1L, 0L, hr);

        verify(applicationRepository).delete(application);
        verify(applicationRepository).flush();
        verify(cvStorageService).delete(application.getCvUrl());
    }

    @Test
    void applicationWithInterviewHistoryIsNotDeleted() {
        Company company = Company.builder().id(10L).name("Example Company").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(interviewRepository.existsByApplicationId(1L)).thenReturn(true);

        assertThrows(ApplicationDeleteConflictException.class,
                () -> applicationService.deleteApplication(1L, 0L, hr));

        verify(applicationRepository, never()).delete(any());
        verify(cvStorageService, never()).delete(any());
        verify(interviewRepository).existsByApplicationId(1L);
    }

    @Test
    void delayedStaleApplicationNotesAreRejectedWithoutChangingNewestValue() {
        Company company = Company.builder().id(10L).name("Example Company").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setVersion(2L);
        application.setHrNotes("newest committed notes");
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> applicationService.updateHrNotes(1L, "stale browser notes", 1L, hr));

        assertEquals("newest committed notes", application.getHrNotes());
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void foreignCompanyHrCannotDeleteApplicationOrStoredCv() {
        Company company = Company.builder().id(10L).name("Company A").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        when(applicationRepository.findByIdAndJobCompany(99L, company)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.deleteApplication(99L, 0L, hr));

        verify(applicationRepository, never()).delete(any());
        verify(applicationRepository, never()).flush();
        verify(cvStorageService, never()).delete(any());
    }

    @Test
    void hrCandidateHistoryUsesCandidateAndCompanyScopedRepositoryQuery() {
        Company company = Company.builder().id(10L).name("Company A").build();
        Company otherCompany = Company.builder().id(11L).name("Company B").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        User candidate = User.builder().id(3L).role(Role.CANDIDATE).build();
        Application ownApplication = Application.builder().version(0L).id(1L).candidate(candidate)
                .job(Job.builder().id(2L).company(company).build()).status(ApplicationStatus.APPLIED)
                .hrNotes("OWN_PRIVATE_NOTES").build();
        Application foreignApplication = Application.builder().version(0L).id(4L).candidate(candidate)
                .job(Job.builder().id(5L).company(otherCompany).build()).status(ApplicationStatus.APPLIED)
                .hrNotes("FOREIGN_PRIVATE_NOTES").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.findByCandidateIdAndJobCompanyOrderByAppliedAtDesc(3L, company))
                .thenReturn(List.of(ownApplication));

        var history = applicationService.getApplicationsByCandidate(3L, hr);

        assertEquals(List.of(1L), history.stream().map(response -> response.id()).toList());
        assertEquals("OWN_PRIVATE_NOTES", history.getFirst().hrNotes());
        assertFalse(history.stream().anyMatch(response ->
                foreignApplication.getHrNotes().equals(response.hrNotes())));
        verify(applicationRepository).findByCandidateIdAndJobCompanyOrderByAppliedAtDesc(3L, company);
        verify(applicationRepository, never()).findByCandidateIdOrderByAppliedAtDesc(3L);
    }

    @Test
    void loadsAuthenticatedCandidatesApplicationsUsingDescendingRepositoryQuery() {
        User candidate = candidate(3L);
        when(applicationRepository.findByCandidateIdOrderByAppliedAtDesc(3L)).thenReturn(List.of());

        assertEquals(List.of(), applicationService.getApplicationsForCandidate(candidate));

        verify(applicationRepository).findByCandidateIdOrderByAppliedAtDesc(3L);
    }

    @Test
    void hrLoadsOnlyApplicationsReturnedForTheirCompany() {
        Company hrCompany = Company.builder().id(10L).name("Shigoto").build();
        Company otherCompany = Company.builder().id(11L).name("Other").build();
        User hr = User.builder().id(20L).role(Role.HR).company(hrCompany).build();
        LocalDateTime appliedAt = LocalDateTime.of(2026, 8, 20, 10, 30);
        Application ownCompanyApplication = Application.builder().version(0L)
                .id(1L).candidate(User.builder().id(3L).firstName("Dana").lastName("Cohen")
                        .role(Role.CANDIDATE).build())
                .job(Job.builder().id(2L).title("Backend Engineer").company(hrCompany).build())
                .status(ApplicationStatus.HR_INTERVIEW).appliedAt(appliedAt).build();
        Application otherCompanyApplication = Application.builder().version(0L)
                .id(2L).candidate(candidate(4L))
                .job(Job.builder().id(3L).company(otherCompany).build()).build();
        Interview completedTechnical = Interview.builder().application(ownCompanyApplication)
                .type(InterviewType.TECHNICAL).status(InterviewStatus.COMPLETED)
                .scheduledAt(LocalDateTime.of(2026, 8, 22, 10, 0)).build();
        Interview scheduledManager = Interview.builder().application(ownCompanyApplication)
                .type(InterviewType.MANAGER).status(InterviewStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.of(2026, 8, 25, 10, 0)).build();
        when(applicationRepository.findByJobCompany(hrCompany)).thenReturn(List.of(ownCompanyApplication));
        when(interviewRepository.findFirstByApplicationIdAndStatusOrderByScheduledAtDesc(
                1L, InterviewStatus.SCHEDULED)).thenReturn(Optional.of(scheduledManager));

        var applications = applicationService.getAllApplications(hr);

        assertEquals(List.of(1L), applications.stream().map(response -> response.applicationId()).toList());
        assertEquals("Dana Cohen", applications.getFirst().candidateName());
        assertEquals("Backend Engineer", applications.getFirst().jobTitle());
        assertEquals(ApplicationStatus.HR_INTERVIEW, applications.getFirst().status());
        assertEquals(appliedAt, applications.getFirst().appliedAt());
        assertEquals(null, applications.getFirst().statusChangedAt());
        assertEquals(InterviewType.MANAGER, applications.getFirst().activeInterviewType());
        assertEquals(InterviewStatus.COMPLETED, completedTechnical.getStatus());
        verify(interviewRepository).findFirstByApplicationIdAndStatusOrderByScheduledAtDesc(
                1L, InterviewStatus.SCHEDULED);
        LocalDateTime realStatusChange = LocalDateTime.of(2026, 8, 23, 9, 15);
        ownCompanyApplication.setStatusChangedAt(realStatusChange);
        assertEquals(realStatusChange,
                HrApplicationSummaryDTO.from(ownCompanyApplication, InterviewType.MANAGER).statusChangedAt());
        verify(applicationRepository).findByJobCompany(hrCompany);
        verify(applicationRepository, never()).findAll();
        org.junit.jupiter.api.Assertions.assertFalse(
                applications.stream().anyMatch(response -> response.applicationId().equals(otherCompanyApplication.getId())));
    }

    @Test
    void hrWithoutCompanyCannotListApplications() {
        User hr = User.builder().id(20L).role(Role.HR).build();

        assertThrows(AccessDeniedException.class, () -> applicationService.getAllApplications(hr));
        verify(applicationRepository, never()).findAll();
    }

    @Test
    void hrFiltersApplicationsByJobWithinTheirCompany() {
        Company company = Company.builder().id(10L).name("Shigoto").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = Application.builder().version(0L)
                .id(1L)
                .candidate(User.builder().id(3L).firstName("Dana").lastName("Cohen")
                        .role(Role.CANDIDATE).build())
                .job(Job.builder().id(2L).title("Backend Engineer").company(company).build())
                .status(ApplicationStatus.APPLIED)
                .build();
        when(applicationRepository.findByJobIdAndJobCompany(2L, company))
                .thenReturn(List.of(application));

        var applications = applicationService.getAllApplications(hr, 2L);

        assertEquals(List.of(1L), applications.stream().map(response -> response.applicationId()).toList());
        verify(applicationRepository).findByJobIdAndJobCompany(2L, company);
        verify(applicationRepository, never()).findByJobId(any());
    }

    @Test
    void foreignCompanyJobFilterReturnsNoApplications() {
        Company company = Company.builder().id(10L).name("Shigoto").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        when(applicationRepository.findByJobIdAndJobCompany(99L, company)).thenReturn(List.of());

        assertTrue(applicationService.getAllApplications(hr, 99L).isEmpty());

        verify(applicationRepository).findByJobIdAndJobCompany(99L, company);
        verify(applicationRepository, never()).findByJobId(any());
    }

    @Test
    void hrReadsOwnCompanyApplicationDetailsWithoutExposingCvStorage() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        var details = applicationService.getHrApplicationDetails(1L, hr);

        assertEquals(1L, details.applicationId());
        assertEquals("Dana", details.firstName());
        assertEquals("Backend Engineer", details.jobTitle());
        org.junit.jupiter.api.Assertions.assertFalse(Arrays.stream(HrApplicationDetailsDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("cvUrl")
                        || component.getName().equals("candidate") || component.getName().equals("job")));
    }

    @Test
    void hrCannotReadOrDownloadAnotherCompanyApplication() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        when(applicationRepository.findByIdAndJobCompany(99L, company)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.getHrApplicationDetails(99L, hr));
        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.getHrApplicationCv(99L, hr));
        verify(cvStorageService, never()).load(any());
    }

    @Test
    void hrDownloadsOwnCompanyCvThroughSecureStorage() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(cvStorageService.load("internal-key.pdf")).thenReturn(new ByteArrayResource("%PDF".getBytes()));

        var download = applicationService.getHrApplicationCv(1L, hr);

        assertEquals("cv-application-1.pdf", download.downloadFilename());
        verify(cvStorageService).load("internal-key.pdf");
    }

    @Test
    void hrWithoutCompanyCannotReadApplicationDetails() {
        User hr = User.builder().id(20L).role(Role.HR).build();

        assertThrows(AccessDeniedException.class,
                () -> applicationService.getHrApplicationDetails(1L, hr));
        verify(applicationRepository, never()).findByIdAndJobCompany(any(), any());
    }

    @Test
    void ownCompanyHrUpdatesNotesWithoutChangingStatus() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        var details = applicationService.updateHrNotes(1L, " Follow up next week ", 0L, hr);

        assertEquals("Follow up next week", details.hrNotes());
        assertEquals(ApplicationStatus.APPLIED, details.status());
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void hrCannotUpdateNotesForAnotherCompanyApplication() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        when(applicationRepository.findByIdAndJobCompany(99L, company)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.updateHrNotes(99L, "Private note", 0L, hr));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void hrCanMoveOwnCompanyApplicationFromAppliedToHrInterview() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var details = applicationService.transitionHrApplicationStatus(
                1L, ApplicationStatus.HR_INTERVIEW, 0L, hr);

        assertEquals(ApplicationStatus.HR_INTERVIEW, details.status());
    }

    @Test
    void hrCanMoveHrInterviewBackToAppliedWhenNoActiveOrCompletedHrInterviewExists() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var details = applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.APPLIED, 0L, hr);

        assertEquals(ApplicationStatus.APPLIED, details.status());
        assertTrue(application.getStatusChangedAt() != null);
        verify(interviewRepository).existsByApplicationIdAndTypeAndStatusNot(
                1L, InterviewType.HR, InterviewStatus.CANCELED);
        verify(notificationEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void hrInterviewCannotMoveBackWhileScheduledOrCompletedHrInterviewExists() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(interviewRepository.existsByApplicationIdAndTypeAndStatusNot(
                1L, InterviewType.HR, InterviewStatus.CANCELED)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.APPLIED, 0L, hr));

        assertEquals(ApplicationStatus.HR_INTERVIEW, application.getStatus());
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void hrInterviewCannotMoveBackWhenHomeTaskDataExists() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        application.setTaskInstructions("Unexpected persisted task");
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.APPLIED, 0L, hr));

        verify(interviewRepository, never()).existsByApplicationIdAndTypeAndStatusNot(any(), any(), any());
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void unsupportedBackwardTransitionsRemainBlocked() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        var unsupported = List.of(
                new ApplicationStatus[]{ApplicationStatus.TASK_SENT, ApplicationStatus.HR_INTERVIEW},
                new ApplicationStatus[]{ApplicationStatus.TASK_SUBMITTED, ApplicationStatus.TASK_SENT},
                new ApplicationStatus[]{ApplicationStatus.TASK_APPROVED, ApplicationStatus.TASK_SUBMITTED},
                new ApplicationStatus[]{ApplicationStatus.TECH_INTERVIEW_SCHEDULED, ApplicationStatus.TASK_APPROVED},
                new ApplicationStatus[]{ApplicationStatus.OFFER, ApplicationStatus.TECH_INTERVIEW_SCHEDULED},
                new ApplicationStatus[]{ApplicationStatus.REJECTED, ApplicationStatus.HR_INTERVIEW});
        for (ApplicationStatus[] transition : unsupported) {
            application.setStatus(transition[0]);
            assertThrows(IllegalArgumentException.class, () -> applicationService.transitionHrApplicationStatus(
                    1L, transition[1], 0L, hr));
        }

        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void hrCannotSkipFromAppliedDirectlyToOffer() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.OFFER, 0L, hr));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void validPreOfferTransitionPublishesOfferedEvent() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);
        when(interviewRepository.existsByApplicationIdAndTypeAndStatus(
                1L, InterviewType.TECHNICAL, InterviewStatus.COMPLETED)).thenReturn(true);

        var details = applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.OFFER, 0L, hr);

        assertEquals(ApplicationStatus.OFFER, details.status());
        verify(notificationEventPublisher).publishAfterCommit(argThat(event ->
                event.type() == NotificationType.APPLICATION_OFFERED
                        && event.applicationId().equals(1L)
                        && event.interviewId() == null));
    }

    @Test
    void scheduledOrCanceledTechnicalInterviewCannotAdvanceToOffer() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TECH_INTERVIEW_SCHEDULED);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(interviewRepository.existsByApplicationIdAndTypeAndStatus(
                1L, InterviewType.TECHNICAL, InterviewStatus.COMPLETED)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.OFFER, 0L, hr));

        assertEquals(ApplicationStatus.TECH_INTERVIEW_SCHEDULED, application.getStatus());
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void offerCanBecomeHiredAndPublishesHiredEvent() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.OFFER);
        application.setStatusChangedAt(LocalDateTime.now().minusDays(1));
        LocalDateTime previousStatusChange = application.getStatusChangedAt();
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var details = applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.HIRED, 0L, hr);

        assertEquals(ApplicationStatus.HIRED, details.status());
        assertTrue(application.getStatusChangedAt().isAfter(previousStatusChange));
        verify(notificationEventPublisher).publishAfterCommit(argThat(event ->
                event.type() == NotificationType.APPLICATION_HIRED
                        && event.applicationId().equals(1L)
                        && event.interviewId() == null));
    }

    @Test
    void onlyOfferCanBecomeHiredAndHiredIsTerminal() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        for (ApplicationStatus status : List.of(ApplicationStatus.APPLIED, ApplicationStatus.HR_INTERVIEW,
                ApplicationStatus.TASK_SENT, ApplicationStatus.TASK_SUBMITTED, ApplicationStatus.TASK_APPROVED,
                ApplicationStatus.TECH_INTERVIEW_SCHEDULED, ApplicationStatus.REJECTED)) {
            application.setStatus(status);
            assertThrows(IllegalArgumentException.class, () -> applicationService.transitionHrApplicationStatus(
                    1L, ApplicationStatus.HIRED, 0L, hr));
        }
        application.setStatus(ApplicationStatus.HIRED);
        assertThrows(IllegalArgumentException.class, () -> applicationService.transitionHrApplicationStatus(
                1L, ApplicationStatus.OFFER, 0L, hr));
        assertThrows(IllegalArgumentException.class, () -> applicationService.transitionHrApplicationStatus(
                1L, ApplicationStatus.REJECTED, 0L, hr));

        verify(applicationRepository, never()).saveAndFlush(any());
        verify(notificationEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void hrCannotApproveSubmittedTask() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () -> applicationService.transitionHrApplicationStatus(
                1L, ApplicationStatus.TASK_APPROVED, 0L, hr));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void interviewerListsOnlyTasksExplicitlyAssignedToThemWithSafeFields() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User interviewer = User.builder().id(30L).role(Role.INTERVIEWER).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        application.setTaskInstructions("Build an API");
        application.setTaskRepoUrl("https://github.com/candidate/task");
        application.setTaskReviewer(interviewer);
        when(applicationRepository.findByStatusAndTaskReviewerIdAndJobCompanyOrderByAppliedAtAsc(
                ApplicationStatus.TASK_SUBMITTED, interviewer.getId(), company)).thenReturn(List.of(application));

        var tasks = applicationService.getSubmittedTasksForInterviewer(interviewer);

        assertEquals(1, tasks.size());
        assertEquals("https://github.com/candidate/task", tasks.getFirst().taskRepoUrl());
        assertEquals("Dana Cohen", tasks.getFirst().candidateName());
        assertEquals(false, Arrays.stream(InterviewerSubmittedTaskDTO.class.getRecordComponents())
                .anyMatch(component -> List.of("hrNotes", "cvUrl", "password", "company")
                        .contains(component.getName())));
    }

    @Test
    void ownCompanyHrRejectsWithTrimmedCandidateFeedback() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.APPLIED);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var response = applicationService.rejectHrApplication(
                1L, "  Thank you for your time. We need deeper API experience.  ", 0L, hr);

        assertEquals(ApplicationStatus.REJECTED, response.status());
        assertEquals("Thank you for your time. We need deeper API experience.", response.candidateFeedback());
    }

    @Test
    void candidateFeedbackIsCompanyScopedValidatedAndEditableOnlyAfterRejection() {
        Company wix = Company.builder().id(10L).name("Wix").build();
        Company google = Company.builder().id(20L).name("Google").build();
        User wixHr = User.builder().id(20L).role(Role.HR).company(wix).build();
        User googleHr = User.builder().id(21L).role(Role.HR).company(google).build();
        when(applicationRepository.findByIdAndJobCompany(1L, google)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.updateCandidateFeedback(1L, "feedback", 0L, googleHr));

        Application rejected = detailedApplication(wix);
        rejected.setStatus(ApplicationStatus.REJECTED);
        when(applicationRepository.findByIdAndJobCompany(1L, wix)).thenReturn(Optional.of(rejected));
        when(applicationRepository.saveAndFlush(rejected)).thenReturn(rejected);
        var cleared = applicationService.updateCandidateFeedback(1L, "   ", 0L, wixHr);
        assertEquals(null, cleared.candidateFeedback());
        assertEquals(ApplicationStatus.REJECTED, rejected.getStatus());
        assertThrows(IllegalArgumentException.class, () ->
                applicationService.updateCandidateFeedback(1L, "x".repeat(10001), 0L, wixHr));

        rejected.setStatus(ApplicationStatus.APPLIED);
        assertThrows(IllegalArgumentException.class, () ->
                applicationService.updateCandidateFeedback(1L, "feedback", 0L, wixHr));
    }

    @Test
    void candidateSeesFeedbackOnlyOnOwnRejectedApplicationWithoutInternalFields() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User owner = User.builder().id(30L).role(Role.CANDIDATE).build();
        User other = User.builder().id(31L).role(Role.CANDIDATE).build();
        Application application = detailedApplication(company);
        application.setCandidate(owner);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setCandidateFeedback("Candidate-safe feedback");
        application.setHrNotes("private HR notes");
        application.setTaskReviewNotes("private task notes");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertEquals("Candidate-safe feedback",
                applicationService.getOwnedApplicationById(1L, owner).candidateFeedback());
        assertThrows(AccessDeniedException.class, () ->
                applicationService.getOwnedApplicationById(1L, other));

        application.setStatus(ApplicationStatus.APPLIED);
        assertEquals(null, applicationService.getOwnedApplicationById(1L, owner).candidateFeedback());
        assertEquals(false, Arrays.stream(ApplicationResponseDTO.class.getRecordComponents())
                .anyMatch(component -> List.of("hrNotes", "feedback", "interviewerNotes", "taskReviewNotes")
                        .contains(component.getName())));
    }

    @Test
    void interviewerApprovesAndRejectsSubmittedTask() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User interviewer = User.builder().id(30L).role(Role.INTERVIEWER).company(company).build();
        Application approved = detailedApplication(company);
        approved.setStatus(ApplicationStatus.TASK_SUBMITTED);
        approved.setTaskReviewer(interviewer);
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, company, interviewer.getId()))
                .thenReturn(Optional.of(approved));
        when(applicationRepository.saveAndFlush(approved)).thenReturn(approved);

        var approval = applicationService.reviewSubmittedTask(1L, TaskReviewDecision.APPROVE, 0L, interviewer);
        assertEquals(ApplicationStatus.TASK_APPROVED, approval.status());

        Application rejected = detailedApplication(company);
        rejected.setId(2L);
        rejected.setStatus(ApplicationStatus.TASK_SUBMITTED);
        rejected.setTaskReviewer(interviewer);
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(2L, company, interviewer.getId()))
                .thenReturn(Optional.of(rejected));
        when(applicationRepository.saveAndFlush(rejected)).thenReturn(rejected);
        var rejection = applicationService.reviewSubmittedTask(2L, TaskReviewDecision.REJECT, 0L, interviewer);
        assertEquals(ApplicationStatus.REJECTED, rejection.status());
    }

    @Test
    void unassignedAndCrossCompanyInterviewersCannotReviewSubmittedTask() {
        Company wix = Company.builder().id(10L).name("Wix").build();
        Company google = Company.builder().id(20L).name("Google").build();
        User assigned = User.builder().id(30L).role(Role.INTERVIEWER).company(wix).build();
        User sameCompanyOther = User.builder().id(31L).role(Role.INTERVIEWER).company(wix).build();
        User otherCompany = User.builder().id(32L).role(Role.INTERVIEWER).company(google).build();
        Application application = detailedApplication(wix);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        application.setTaskReviewer(assigned);

        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, wix, 31L))
                .thenReturn(Optional.empty());
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, google, 32L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> applicationService.reviewSubmittedTask(
                1L, TaskReviewDecision.APPROVE, 0L, sameCompanyOther));
        assertThrows(ResourceNotFoundException.class, () -> applicationService.reviewSubmittedTask(
                1L, TaskReviewDecision.REJECT, 0L, otherCompany));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void approvingLegacyTaskWithActiveTechnicalInterviewPreservesInterviewStage() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User interviewer = User.builder().id(30L).role(Role.INTERVIEWER).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        application.setTaskReviewer(interviewer);
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, company, 30L))
                .thenReturn(Optional.of(application));
        when(interviewRepository.existsByApplicationIdAndTypeAndStatus(
                1L, InterviewType.TECHNICAL, InterviewStatus.SCHEDULED)).thenReturn(true);
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var result = applicationService.reviewSubmittedTask(
                1L, TaskReviewDecision.APPROVE, 0L, interviewer);

        assertEquals(ApplicationStatus.TECH_INTERVIEW_SCHEDULED, result.status());
    }

    @Test
    void companyInterviewerSavesTrimmedTaskReviewNotesWithoutChangingStatus() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User interviewer = User.builder().id(30L).role(Role.INTERVIEWER).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        application.setTaskReviewer(interviewer);
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, company, interviewer.getId()))
                .thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var response = applicationService.updateTaskReviewNotes(
                1L, "  Check error handling and test coverage.  ", 0L, interviewer);

        assertEquals("Check error handling and test coverage.", response.taskReviewNotes());
        assertEquals(ApplicationStatus.TASK_SUBMITTED, application.getStatus());
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void taskReviewNotesRejectOtherCompanyInvalidRoleAndExcessLength() {
        Company wix = Company.builder().id(10L).name("Wix").build();
        Company google = Company.builder().id(20L).name("Google").build();
        User googleInterviewer = User.builder().id(31L).role(Role.INTERVIEWER).company(google).build();
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, google, 31L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.updateTaskReviewNotes(1L, "notes", 0L, googleInterviewer));
        assertThrows(AccessDeniedException.class, () ->
                applicationService.updateTaskReviewNotes(1L, "notes", 0L,
                        User.builder().role(Role.CANDIDATE).build()));

        User wixInterviewer = User.builder().id(30L).role(Role.INTERVIEWER).company(wix).build();
        Application application = detailedApplication(wix);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        application.setTaskReviewer(wixInterviewer);
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, wix, 30L))
                .thenReturn(Optional.of(application));
        assertThrows(IllegalArgumentException.class, () ->
                applicationService.updateTaskReviewNotes(1L, "x".repeat(10001), 0L, wixInterviewer));
    }

    @Test
    void candidateApplicationDtoDoesNotExposeTaskReviewNotes() {
        assertEquals(false, Arrays.stream(ApplicationResponseDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("taskReviewNotes")));
    }

    @Test
    void interviewerCannotReviewInvalidStateTwiceOrAcrossCompany() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User interviewer = User.builder().id(30L).role(Role.INTERVIEWER).company(company).build();
        Application reviewed = detailedApplication(company);
        reviewed.setStatus(ApplicationStatus.TASK_APPROVED);
        reviewed.setTaskReviewer(interviewer);
        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(1L, company, 30L))
                .thenReturn(Optional.of(reviewed));
        assertThrows(IllegalArgumentException.class, () -> applicationService.reviewSubmittedTask(
                1L, TaskReviewDecision.REJECT, 0L, interviewer));

        when(applicationRepository.findByIdAndJobCompanyAndTaskReviewerId(99L, company, 30L))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> applicationService.reviewSubmittedTask(
                99L, TaskReviewDecision.APPROVE, 0L, interviewer));
    }

    @Test
    void interviewerWithoutCompanyIsRejected() {
        User interviewer = User.builder().id(30L).role(Role.INTERVIEWER).build();
        assertThrows(AccessDeniedException.class, () ->
                applicationService.getSubmittedTasksForInterviewer(interviewer));
        assertThrows(AccessDeniedException.class, () -> applicationService.reviewSubmittedTask(
                1L, TaskReviewDecision.APPROVE, 0L, interviewer));
        verify(applicationRepository, never()).findByIdAndJobCompanyAndTaskReviewerId(any(), any(), any());
    }

    @Test
    void hrCannotChangeAnotherCompanyApplicationStatus() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        when(applicationRepository.findByIdAndJobCompany(99L, company)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.transitionHrApplicationStatus(99L, ApplicationStatus.HIRED, 0L, hr));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void hrWithoutCompanyCannotChangeApplicationStatusOrAssignTask() {
        User hr = User.builder().id(20L).role(Role.HR).build();

        assertThrows(AccessDeniedException.class, () ->
                applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.REJECTED, 0L, hr));
        assertThrows(AccessDeniedException.class, () ->
                applicationService.assignHomeTask(1L, "Build an API", LocalDateTime.now().plusDays(2), 30L, 0L, hr));
        verify(applicationRepository, never()).findByIdAndJobCompany(any(), any());
    }

    @Test
    void nonHrCannotChangeApplicationStatus() {
        User candidate = User.builder().id(20L).role(Role.CANDIDATE).build();

        assertThrows(AccessDeniedException.class, () ->
                applicationService.transitionHrApplicationStatus(1L, ApplicationStatus.HR_INTERVIEW, 0L, candidate));

        verify(applicationRepository, never()).findByIdAndJobCompany(any(), any());
    }

    @Test
    void hrAssignsFutureHomeTaskFromHrInterview() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        application.setTaskRepoUrl("https://github.com/old/submission");
        LocalDateTime deadline = LocalDateTime.now().plusDays(5);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        User reviewer = User.builder().id(30L).role(Role.INTERVIEWER).company(company).build();
        when(userRepository.findByIdAndCompany(30L, company)).thenReturn(Optional.of(reviewer));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        var details = applicationService.assignHomeTask(1L, "  Build a REST API  ", deadline, 30L, 0L, hr);

        assertEquals(ApplicationStatus.TASK_SENT, details.status());
        assertEquals(deadline, details.taskDeadline());
        assertEquals("Build a REST API", details.taskInstructions());
        assertEquals(null, details.taskRepoUrl());
        assertEquals(30L, details.taskReviewerId());
    }

    @Test
    void homeTaskRejectsPastDeadlineAndDuplicateSend() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.assignHomeTask(1L, "Build an API", LocalDateTime.now().minusMinutes(1), 30L, 0L, hr));

        application.setStatus(ApplicationStatus.TASK_SENT);
        assertThrows(IllegalArgumentException.class, () ->
                applicationService.assignHomeTask(1L, "Build an API", LocalDateTime.now().plusDays(2), 30L, 0L, hr));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void homeTaskCannotOverwriteSubmittedTask() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        application.setTaskRepoUrl("https://github.com/candidate/submission");
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () ->
                applicationService.assignHomeTask(1L, "Build an API", LocalDateTime.now().plusDays(2), 30L, 0L, hr));
        assertEquals("https://github.com/candidate/submission", application.getTaskRepoUrl());
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void homeTaskRejectsBlankInstructions() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () -> applicationService.assignHomeTask(
                1L, "   ", LocalDateTime.now().plusDays(2), 30L, 0L, hr));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void hrUpdatesAssignedTaskDeadlineWithoutChangingWorkflowOrTaskData() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        LocalDateTime statusChangedAt = LocalDateTime.now().minusDays(1);
        application.setStatus(ApplicationStatus.TASK_SENT);
        application.setStatusChangedAt(statusChangedAt);
        application.setTaskInstructions("Build the existing API");
        application.setTaskRepoUrl("https://github.com/candidate/existing");
        application.setTaskReviewNotes("PRIVATE_REVIEW");
        LocalDateTime updatedDeadline = LocalDateTime.now().plusDays(4);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        HrApplicationDetailsDTO details = applicationService.updateHomeTaskDeadline(
                1L, updatedDeadline, 0L, hr);

        assertEquals(updatedDeadline, details.taskDeadline());
        assertEquals(ApplicationStatus.TASK_SENT, application.getStatus());
        assertEquals(statusChangedAt, application.getStatusChangedAt());
        assertEquals("Build the existing API", application.getTaskInstructions());
        assertEquals("https://github.com/candidate/existing", application.getTaskRepoUrl());
        assertEquals("PRIVATE_REVIEW", application.getTaskReviewNotes());
        var event = org.mockito.ArgumentCaptor.forClass(
                com.shigoto.backend.messaging.CandidateNotificationEvent.class);
        verify(notificationEventPublisher).publishAfterCommit(event.capture());
        assertEquals(com.shigoto.backend.entity.NotificationType.HOME_TASK_UPDATED,
                event.getValue().type());
    }

    @Test
    void deadlineUpdateRejectsInvalidDeadlineAndPostSubmissionStatuses() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        Application application = detailedApplication(company);
        application.setStatus(ApplicationStatus.TASK_SENT);
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.updateHomeTaskDeadline(1L, null, 0L, hr));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.updateHomeTaskDeadline(1L, LocalDateTime.now().minusMinutes(1), 0L, hr));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.updateHomeTaskDeadline(1L, LocalDateTime.now(), 0L, hr));
        for (ApplicationStatus status : List.of(ApplicationStatus.TASK_SUBMITTED,
                ApplicationStatus.TASK_APPROVED, ApplicationStatus.TECH_INTERVIEW_SCHEDULED)) {
            application.setStatus(status);
            assertThrows(IllegalArgumentException.class, () -> applicationService.updateHomeTaskDeadline(
                    1L, LocalDateTime.now().plusDays(2), 0L, hr));
        }
        verify(applicationRepository, never()).saveAndFlush(any());
        verify(notificationEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void deadlineUpdatePreservesHrRoleAndCompanyScoping() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        when(applicationRepository.findByIdAndJobCompany(1L, company)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> applicationService.updateHomeTaskDeadline(
                1L, LocalDateTime.now().plusDays(2), 0L, hr));
        assertThrows(AccessDeniedException.class, () -> applicationService.updateHomeTaskDeadline(
                1L, LocalDateTime.now().plusDays(2), 0L, User.builder().role(Role.CANDIDATE).build()));
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    private Application assignedApplication(LocalDateTime deadline) {
        Company company = Company.builder().name("Example Company").build();
        Job job = Job.builder().id(2L).title("Developer").company(company).location("Remote").build();
        User candidate = candidate(3L);
        return Application.builder().version(0L)
                .id(1L)
                .candidate(candidate)
                .job(job)
                .status(ApplicationStatus.TASK_SENT)
                .taskDeadline(deadline)
                .taskInstructions("Implement the documented API")
                .build();
    }

    private Application detailedApplication(Company company) {
        User candidate = User.builder().id(3L).firstName("Dana").lastName("Cohen")
                .email("dana@example.com").role(Role.CANDIDATE).build();
        Job job = Job.builder().id(2L).title("Backend Engineer").location("Remote").company(company).build();
        return Application.builder().version(0L).id(1L).candidate(candidate).job(job).status(ApplicationStatus.APPLIED)
                .coverLetter("Cover").hrNotes("Notes").cvUrl("internal-key.pdf")
                .appliedAt(LocalDateTime.of(2026, 8, 20, 10, 30)).build();
    }

    private User candidate(Long id) {
        return User.builder().id(id).role(Role.CANDIDATE).build();
    }

    private MockMultipartFile validCv() {
        return new MockMultipartFile(
                "cv", "resume.pdf", "application/pdf", "%PDF-1.4\ntest".getBytes());
    }
}
