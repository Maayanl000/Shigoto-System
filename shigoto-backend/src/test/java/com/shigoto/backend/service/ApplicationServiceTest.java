package com.shigoto.backend.service;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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

        var response = applicationService.submitTask(1L, " https://github.com/example/home-task ");

        assertEquals(ApplicationStatus.TASK_SUBMITTED, response.status());
        assertEquals("https://github.com/example/home-task", response.taskRepoUrl());
        verify(applicationRepository).save(application);
    }

    @Test
    void rejectsMissingApplication() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> applicationService.submitTask(99L, "https://github.com/example/repo"));
    }

    @Test
    void rejectsApplicationThatHasNotReachedTaskStage() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setStatus(ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://github.com/example/repo"));
        verify(applicationRepository, never()).save(application);
    }

    @Test
    void rejectsSecondSubmission() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://github.com/example/repo"));
    }

    @Test
    void rejectsMissingOrExpiredDeadline() {
        Application application = assignedApplication(null);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://github.com/example/repo"));

        application.setTaskDeadline(LocalDateTime.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://github.com/example/repo"));
    }

    @Test
    void rejectsMissingMalformedNonGithubAndProfileUrls() {
        Application application = assignedApplication(LocalDateTime.now().plusHours(1));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, null));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, " "));
        assertThrows(IllegalArgumentException.class, () -> applicationService.submitTask(1L, "not a url"));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://example.com/user/repo"));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.submitTask(1L, "https://github.com/user"));
    }

    private Application assignedApplication(LocalDateTime deadline) {
        Company company = Company.builder().name("Example Company").build();
        Job job = Job.builder().id(2L).title("Developer").company(company).location("Remote").build();
        User candidate = User.builder().id(3L).build();
        return Application.builder()
                .id(1L)
                .candidate(candidate)
                .job(job)
                .status(ApplicationStatus.TASK_SENT)
                .taskDeadline(deadline)
                .build();
    }
}
