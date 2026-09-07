package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Declares application-specific persistence queries for job data.
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    /**
     * Lists jobs in the requested publication status.
     * @param status the requested domain status
     * @return jobs whose status matches the supplied value
     */
    List<Job> findByStatus(JobStatus status);

    /**
     * Lists a company's jobs from newest to oldest.
     * @param company the company that scopes the operation
     * @return company-owned jobs ordered by creation time descending
     */
    List<Job> findByCompanyOrderByCreatedAtDesc(Company company);

    /**
     * Finds a job by identifier within a company boundary.
     * @param id the entity identifier
     * @param company the company that scopes the operation
     * @return the company-owned job, if present
     */
    Optional<Job> findByIdAndCompany(Long id, Company company);

    /**
     * Finds a company job with the supplied title.
     * @param company the company that scopes the operation
     * @param title the title
     * @return the matching company job, if present
     */
    Optional<Job> findByCompanyAndTitle(Company company, String title);
}
