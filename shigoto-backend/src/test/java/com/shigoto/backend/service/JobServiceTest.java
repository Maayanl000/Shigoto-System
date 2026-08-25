package com.shigoto.backend.service;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.repository.JobRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest {

    @Test
    void publicJobsContainOpenJobsAndExcludeNonOpenJobs() {
        JobRepository repository = mock(JobRepository.class);
        Job open = Job.builder().id(1L).status(JobStatus.OPEN).build();
        JobService service = new JobService(repository);
        when(repository.findByStatus(JobStatus.OPEN)).thenReturn(List.of(open));

        List<Job> result = service.getOpenJobs();

        assertEquals(List.of(open), result);
        assertFalse(result.stream().anyMatch(job -> job.getStatus() != JobStatus.OPEN));
        verify(repository).findByStatus(JobStatus.OPEN);
    }
}
