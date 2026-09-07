package com.shigoto.backend.service;

import com.shigoto.backend.dto.HrJobCreateRequestDTO;
import com.shigoto.backend.dto.HrJobResponseDTO;
import com.shigoto.backend.dto.HrJobUpdateRequestDTO;
import com.shigoto.backend.dto.PublicJobResponseDTO;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lists public vacancies and manages company-scoped jobs for authenticated HR users.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    /**
     * Lists all jobs currently open to public candidates.
     * @return public representations of all open jobs
     */
    public List<PublicJobResponseDTO> getOpenJobs() {
        return jobRepository.findByStatus(JobStatus.OPEN).stream()
                .map(PublicJobResponseDTO::from)
                .toList();
    }

    /**
     * Lists jobs owned by the authenticated HR user's company, newest first.
     * @param hr the authenticated HR user defining company scope
     * @return HR-facing job DTOs for the authenticated user's company
     */
    public List<HrJobResponseDTO> getJobsForHr(User hr) {
        requireHrWithCompany(hr);
        return jobRepository.findByCompanyOrderByCreatedAtDesc(hr.getCompany()).stream()
                .map(HrJobResponseDTO::from)
                .toList();
    }

    /**
     * Creates an open job for the authenticated HR user's company from validated request fields.
     * @param hr the authenticated HR user defining company scope
     * @param request the request payload
     * @return an HR-facing DTO for the persisted job
     */
    public HrJobResponseDTO createJobForHr(User hr, HrJobCreateRequestDTO request) {
        requireHrWithCompany(hr);
        if (request == null) {
            throw new IllegalArgumentException("Job details are required");
        }
        Job job = Job.builder()
                .title(requireText(request.title(), "Title", 255))
                .description(requireText(request.description(), "Description"))
                .location(requireText(request.location(), "Location", 255))
                .status(JobStatus.OPEN)
                .company(hr.getCompany())
                .build();
        return HrJobResponseDTO.from(jobRepository.save(job));
    }

    /**
     * Updates a company-owned job after validating request fields and the client-supplied version.
     * @param hr the authenticated HR user defining company scope
     * @param jobId the job identifier
     * @param request the request payload
     * @return an HR-facing DTO for the updated job
     */
    @Transactional
    public HrJobResponseDTO updateJobForHr(User hr, Long jobId, HrJobUpdateRequestDTO request) {
        requireHrWithCompany(hr);
        if (request == null) {
            throw new IllegalArgumentException("Job details are required");
        }
        if (request.status() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (request.version() == null) {
            throw new IllegalArgumentException("Job version is required");
        }
        Job job = jobRepository.findByIdAndCompany(jobId, hr.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!request.version().equals(job.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Job.class, jobId);
        }
        job.setTitle(requireText(request.title(), "Title", 255));
        job.setDescription(requireText(request.description(), "Description"));
        job.setLocation(requireText(request.location(), "Location", 255));
        job.setStatus(request.status());
        return HrJobResponseDTO.from(jobRepository.saveAndFlush(job));
    }

    /**
     * Requires an HR user with an assigned company so job operations remain company-scoped.
     * @param hr the authenticated HR user defining company scope
     */
    private void requireHrWithCompany(User hr) {
        if (hr == null || hr.getRole() != Role.HR) {
            throw new AccessDeniedException("HR access is required");
        }
        if (hr.getCompany() == null) {
            throw new AccessDeniedException("HR user must belong to a company");
        }
    }

    /**
     * Requires non-blank text and returns its trimmed form.
     * @param value the value to validate or normalize
     * @param fieldName the field name used in validation errors
     * @return the validated, trimmed text
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * Requires non-blank text whose trimmed length does not exceed the supplied maximum.
     * @param value the value to validate or normalize
     * @param fieldName the field name used in validation errors
     * @param maximumLength the maximum permitted output length
     * @return the validated, trimmed text
     */
    private String requireText(String value, String fieldName, int maximumLength) {
        String trimmed = requireText(value, fieldName);
        if (trimmed.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maximumLength + " characters");
        }
        return trimmed;
    }
}
