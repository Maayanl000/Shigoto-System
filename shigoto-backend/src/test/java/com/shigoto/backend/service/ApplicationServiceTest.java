package com.shigoto.backend.service;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.DuplicateApplicationException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest {

    private ApplicationRepository applicationRepository;
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        applicationService = new ApplicationService(
                applicationRepository, mock(UserRepository.class), mock(JobRepository.class));
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
        Job job = Job.builder().id(2L).status(JobStatus.OPEN).build();
        JobRepository jobRepository = mock(JobRepository.class);
        applicationService = new ApplicationService(
                applicationRepository, mock(UserRepository.class), jobRepository);
        when(jobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByCandidateIdAndJobId(3L, 2L)).thenReturn(false);
        when(applicationRepository.save(org.mockito.ArgumentMatchers.any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application created = applicationService.createApplication(candidate, 2L, null, "Cover note");

        assertEquals(candidate, created.getCandidate());
        assertEquals("Cover note", created.getCoverLetter());

        when(applicationRepository.existsByCandidateIdAndJobId(3L, 2L)).thenReturn(true);
        assertThrows(DuplicateApplicationException.class,
                () -> applicationService.createApplication(candidate, 2L, null, "Cover note"));
    }

    @Test
    void loadsAuthenticatedCandidatesApplicationsUsingDescendingRepositoryQuery() {
        User candidate = candidate(3L);
        when(applicationRepository.findByCandidateIdOrderByAppliedAtDesc(3L)).thenReturn(List.of());

        assertEquals(List.of(), applicationService.getApplicationsForCandidate(candidate));

        verify(applicationRepository).findByCandidateIdOrderByAppliedAtDesc(3L);
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

    private User candidate(Long id) {
        return User.builder().id(id).role(Role.CANDIDATE).build();
    }
}
