package com.shigoto.backend.service;

import com.shigoto.backend.dto.HrJobCreateRequestDTO;
import com.shigoto.backend.dto.HrJobUpdateRequestDTO;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest {

    private JobRepository repository;
    private JobService service;

    @BeforeEach
    void setUp() {
        repository = mock(JobRepository.class);
        service = new JobService(repository);
    }

    @Test
    void publicJobsContainOpenJobsAndExcludeNonOpenJobs() {
        Company company = company(1L, "Wix");
        Job open = job(1L, company, JobStatus.OPEN);
        when(repository.findByStatus(JobStatus.OPEN)).thenReturn(List.of(open));

        var result = service.getOpenJobs();

        assertEquals(List.of(1L), result.stream().map(response -> response.id()).toList());
        assertEquals("Wix", result.getFirst().companyName());
        assertFalse(result.stream().anyMatch(job -> job.status() != JobStatus.OPEN));
        verify(repository).findByStatus(JobStatus.OPEN);
    }

    @Test
    void hrListsOnlyJobsQueriedForOwnCompany() {
        Company wix = company(1L, "Wix");
        User hr = hr(wix);
        Job wixJob = job(2L, wix, JobStatus.PAUSED);
        when(repository.findByCompanyOrderByCreatedAtDesc(wix)).thenReturn(List.of(wixJob));

        var result = service.getJobsForHr(hr);

        assertEquals(List.of(2L), result.stream().map(response -> response.id()).toList());
        verify(repository).findByCompanyOrderByCreatedAtDesc(wix);
        verify(repository, never()).findAll();
    }

    @Test
    void hrCreatesOpenJobForOwnCompany() {
        Company google = company(10L, "Google");
        User hr = hr(google);
        when(repository.save(any(Job.class))).thenAnswer(invocation -> {
            Job saved = invocation.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        var response = service.createJobForHr(
                hr, new HrJobCreateRequestDTO(" Backend Engineer ", " Build APIs ", " Hybrid "));

        assertEquals(8L, response.id());
        assertEquals(JobStatus.OPEN, response.status());
        verify(repository).save(org.mockito.ArgumentMatchers.argThat(job ->
                job.getCompany() == google
                        && job.getStatus() == JobStatus.OPEN
                        && "Backend Engineer".equals(job.getTitle())));
    }

    @Test
    void hrCannotEditAnotherCompanyJob() {
        Company wix = company(1L, "Wix");
        when(repository.findByIdAndCompany(50L, wix)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateJobForHr(
                hr(wix), 50L, update(JobStatus.PAUSED)));
        verify(repository, never()).save(any());
    }

    @Test
    void hrCanEditOwnJobWithoutChangingCompany() {
        Company wix = company(1L, "Wix");
        Job job = job(3L, wix, JobStatus.OPEN);
        when(repository.findByIdAndCompany(3L, wix)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);

        var response = service.updateJobForHr(hr(wix), 3L,
                new HrJobUpdateRequestDTO("Updated", "Updated description", "Remote", JobStatus.PAUSED));

        assertEquals("Updated", response.title());
        assertEquals(JobStatus.PAUSED, response.status());
        assertSame(wix, job.getCompany());
        verify(repository).save(job);
    }

    @Test
    void hrCanCloseOwnJob() {
        Company wix = company(1L, "Wix");
        Job job = job(3L, wix, JobStatus.OPEN);
        when(repository.findByIdAndCompany(3L, wix)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);

        var response = service.updateJobForHr(hr(wix), 3L, update(JobStatus.CLOSED));

        assertEquals(JobStatus.CLOSED, response.status());
        assertEquals(JobStatus.CLOSED, job.getStatus());
    }

    @Test
    void hrWithoutCompanyIsRejected() {
        User hr = User.builder().role(Role.HR).build();

        assertThrows(AccessDeniedException.class, () -> service.getJobsForHr(hr));
        assertThrows(AccessDeniedException.class, () -> service.createJobForHr(
                hr, new HrJobCreateRequestDTO("Title", "Description", "Location")));
        verify(repository, never()).findAll();
    }

    private HrJobUpdateRequestDTO update(JobStatus status) {
        return new HrJobUpdateRequestDTO("Developer", "Description", "Remote", status);
    }

    private Company company(Long id, String name) {
        return Company.builder().id(id).name(name).build();
    }

    private User hr(Company company) {
        return User.builder().id(20L).role(Role.HR).company(company).build();
    }

    private Job job(Long id, Company company, JobStatus status) {
        return Job.builder().id(id).title("Developer").description("Description")
                .location("Remote").company(company).status(status).build();
    }
}
