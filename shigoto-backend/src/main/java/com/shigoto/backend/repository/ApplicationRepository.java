package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Declares application-specific persistence queries for application data.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * Lists a candidate's applications from newest to oldest.
     * @param candidateId the candidate identifier
     * @return applications owned by the candidate, ordered by application time descending
     */
    List<Application> findByCandidateIdOrderByAppliedAtDesc(Long candidateId);

    /**
     * Lists a candidate's applications for jobs owned by a company, newest first.
     * @param candidateId the candidate identifier
     * @param company the company that scopes the operation
     * @return company-scoped applications owned by the candidate
     */
    List<Application> findByCandidateIdAndJobCompanyOrderByAppliedAtDesc(
            Long candidateId, Company company);

    /**
     * Lists all applications submitted for a job.
     * @param jobId the job identifier
     * @return applications submitted for the job
     */
    List<Application> findByJobId(Long jobId);

    /**
     * Lists applications submitted to jobs owned by a company.
     * @param company the company that scopes the operation
     * @return applications within the company
     */
    List<Application> findByJobCompany(Company company);

    /**
     * Lists applications for a job only when the job belongs to the supplied company.
     * @param jobId the job identifier
     * @param company the company that scopes the operation
     * @return applications for the company-owned job
     */
    List<Application> findByJobIdAndJobCompany(Long jobId, Company company);

    /**
     * Finds an application by identifier within a company boundary.
     * @param id the entity identifier
     * @param company the company that scopes the operation
     * @return the company-owned application, if present
     */
    Optional<Application> findByIdAndJobCompany(Long id, Company company);

    /**
     * Lists company applications in a workflow status, oldest submissions first.
     * @param status the requested domain status
     * @param company the company that scopes the operation
     * @return company applications in the requested status
     */
    List<Application> findByStatusAndJobCompanyOrderByAppliedAtAsc(
            ApplicationStatus status, Company company);

    /**
     * Lists company applications in a workflow status assigned to a task reviewer.
     * @param status the requested domain status
     * @param taskReviewerId the task reviewer id
     * @param company the company that scopes the operation
     * @return reviewer-assigned applications in the requested status, oldest first
     */
    List<Application> findByStatusAndTaskReviewerIdAndJobCompanyOrderByAppliedAtAsc(
            ApplicationStatus status, Long taskReviewerId, Company company);

    /**
     * Finds an application by identifier, company, and assigned task reviewer.
     * @param id the entity identifier
     * @param company the company that scopes the operation
     * @param taskReviewerId the task reviewer id
     * @return the reviewer-assigned company application, if present
     */
    Optional<Application> findByIdAndJobCompanyAndTaskReviewerId(
            Long id, Company company, Long taskReviewerId);

    /**
     * Checks whether a candidate has already applied to a job.
     * @param candidateId the candidate identifier
     * @param jobId the job identifier
     * @return {@code true} when an application exists for the candidate and job; otherwise {@code false}
     */
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);

    /**
     * Finds the application connecting a candidate and job.
     * @param candidateId the candidate identifier
     * @param jobId the job identifier
     * @return the candidate's application for the job, if present
     */
    Optional<Application> findByCandidateIdAndJobId(Long candidateId, Long jobId);
}
