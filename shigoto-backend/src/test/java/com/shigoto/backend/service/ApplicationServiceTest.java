package com.shigoto.backend.service;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.dto.HrApplicationDetailsDTO;
import com.shigoto.backend.exception.DuplicateApplicationException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest {

    private ApplicationRepository applicationRepository;
    private ApplicationService applicationService;
    private CvStorageService cvStorageService;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        cvStorageService = mock(CvStorageService.class);
        applicationService = new ApplicationService(
                applicationRepository, mock(UserRepository.class), mock(JobRepository.class), cvStorageService);
    }

    @Test
    void submitsTaskAndAdvancesStatus() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        var response = applicationService.submitTask(
                1L, " https://github.com/example/home-task ", application.getCandidate());

        assertEquals(ApplicationStatus.TASK_SUBMITTED, response.status());
        assertEquals("https://github.com/example/home-task", response.taskRepoUrl());
        verify(applicationRepository).save(application);
    }

    @Test
    void rejectsMissingApplication() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.submitTask(99L, "https://github.com/example/repo", candidate(3L)));
    }

    @Test
    void rejectsApplicationThatHasNotReachedTaskStage() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setStatus(ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(
                        1L, "https://github.com/example/repo", application.getCandidate()));
        verify(applicationRepository, never()).save(application);
    }

    @Test
    void rejectsSecondSubmission() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(
                        1L, "https://github.com/example/repo", application.getCandidate()));
    }

    @Test
    void rejectsMissingOrExpiredDeadline() {
        Application application = assignedApplication(null);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(
                1L, "https://github.com/example/repo", application.getCandidate()));

        application.setTaskDeadline(LocalDateTime.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(
                1L, "https://github.com/example/repo", application.getCandidate()));
    }

    @Test
    void rejectsMissingMalformedNonGithubAndProfileUrls() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        User candidate = application.getCandidate();
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, null, candidate));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, " ", candidate));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, "not a url", candidate));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://example.com/user/repo", candidate));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://github.com/user", candidate));
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
                        1L, "https://github.com/example/repo", otherCandidate));
        verify(applicationRepository, never()).save(application);
    }

    @Test
    void createsApplicationForAuthenticatedCandidateAndPreservesDuplicateProtection() {
        User candidate = candidate(3L);
        Job job = Job.builder().id(2L).title("Developer").location("Remote")
                .company(Company.builder().name("Example Company").build())
                .status(JobStatus.OPEN).build();
        JobRepository jobRepository = mock(JobRepository.class);
        applicationService = new ApplicationService(
                applicationRepository, mock(UserRepository.class), jobRepository, cvStorageService);
        when(jobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByCandidateIdAndJobId(3L, 2L)).thenReturn(false);
        when(cvStorageService.store(any())).thenReturn("123e4567-e89b-12d3-a456-426614174000.pdf");
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile cv = validCv();

        var created = applicationService.createApplication(candidate, 2L, "Cover note", cv);

        assertEquals(candidate.getId(), created.candidateId());
        assertEquals("Cover note", created.coverLetter());
        verify(applicationRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(application ->
                "123e4567-e89b-12d3-a456-426614174000.pdf".equals(application.getCvUrl())));

        when(applicationRepository.existsByCandidateIdAndJobId(3L, 2L)).thenReturn(true);
        assertThrows(DuplicateApplicationException.class,
                () -> applicationService.createApplication(candidate, 2L, "Cover note", cv));
    }

    @Test
    void deletesStoredCvWhenApplicationDatabaseSaveFails() {
        User candidate = candidate(3L);
        Job job = Job.builder().id(2L).status(JobStatus.OPEN).build();
        JobRepository jobRepository = mock(JobRepository.class);
        applicationService = new ApplicationService(
                applicationRepository, mock(UserRepository.class), jobRepository, cvStorageService);
        String storageKey = "123e4567-e89b-12d3-a456-426614174000.pdf";
        when(jobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(cvStorageService.store(any())).thenReturn(storageKey);
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateApplicationException.class,
                () -> applicationService.createApplication(candidate, 2L, "Cover", validCv()));

        verify(cvStorageService).delete(storageKey);
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
    void deletingApplicationAlsoDeletesStoredCv() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setCvUrl("123e4567-e89b-12d3-a456-426614174000.pdf");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(1L);

        verify(applicationRepository).delete(application);
        verify(applicationRepository).flush();
        verify(cvStorageService).delete(application.getCvUrl());
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
        Application ownCompanyApplication = Application.builder()
                .id(1L).candidate(User.builder().id(3L).firstName("Dana").lastName("Cohen")
                        .role(Role.CANDIDATE).build())
                .job(Job.builder().id(2L).title("Backend Engineer").company(hrCompany).build())
                .status(ApplicationStatus.HR_INTERVIEW).appliedAt(appliedAt).build();
        Application otherCompanyApplication = Application.builder()
                .id(2L).candidate(candidate(4L))
                .job(Job.builder().id(3L).company(otherCompany).build()).build();
        when(applicationRepository.findByJobCompany(hrCompany)).thenReturn(List.of(ownCompanyApplication));

        var applications = applicationService.getAllApplications(hr);

        assertEquals(List.of(1L), applications.stream().map(response -> response.applicationId()).toList());
        assertEquals("Dana Cohen", applications.getFirst().candidateName());
        assertEquals("Backend Engineer", applications.getFirst().jobTitle());
        assertEquals(ApplicationStatus.HR_INTERVIEW, applications.getFirst().status());
        assertEquals(appliedAt, applications.getFirst().appliedAt());
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
        when(applicationRepository.save(application)).thenReturn(application);

        var details = applicationService.updateHrNotes(1L, " Follow up next week ", hr);

        assertEquals("Follow up next week", details.hrNotes());
        assertEquals(ApplicationStatus.APPLIED, details.status());
        verify(applicationRepository).save(application);
    }

    @Test
    void hrCannotUpdateNotesForAnotherCompanyApplication() {
        Company company = Company.builder().id(10L).name("Wix").build();
        User hr = User.builder().id(20L).role(Role.HR).company(company).build();
        when(applicationRepository.findByIdAndJobCompany(99L, company)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.updateHrNotes(99L, "Private note", hr));
        verify(applicationRepository, never()).save(any());
    }

    private Application assignedApplication(LocalDateTime deadline) {
        Company company = Company.builder().name("Example Company").build();
        Job job = Job.builder().id(2L).title("Developer").company(company).location("Remote").build();
        User candidate = candidate(3L);
        return Application.builder()
                .id(1L)
                .candidate(candidate)
                .job(job)
                .status(ApplicationStatus.TASK_SENT)
                .taskDeadline(deadline)
                .build();
    }

    private Application detailedApplication(Company company) {
        User candidate = User.builder().id(3L).firstName("Dana").lastName("Cohen")
                .email("dana@example.com").role(Role.CANDIDATE).build();
        Job job = Job.builder().id(2L).title("Backend Engineer").location("Remote").company(company).build();
        return Application.builder().id(1L).candidate(candidate).job(job).status(ApplicationStatus.APPLIED)
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
