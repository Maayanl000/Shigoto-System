package com.shigoto.backend.service;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus; // הנה ה-import הנקי שהוספנו!
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.exception.DuplicateApplicationException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    public Application createApplication(Long candidateId, Long jobId, String cvUrl, String coverLetter) {
        var candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new IllegalArgumentException("Referenced user is not a candidate");
        }

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalArgumentException("Job is not open for applications");
        }

        if (applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
            throw new DuplicateApplicationException("Candidate has already applied for this job");
        }

        Application application = Application.builder()
                .candidate(candidate)
                .job(job)
                .cvUrl(cvUrl)
                .coverLetter(coverLetter)
                .build();

        try {
            return applicationRepository.save(application);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateApplicationException("Candidate has already applied for this job");
        }
    }


    // פונקציית העדכון המעודכנת והנקייה שלנו
    public Application updateApplicationStatus(Long applicationId, ApplicationStatus newStatus, String hrNotes) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with id: " + applicationId));

        if (newStatus != null) {
            application.setStatus(newStatus);
        }
        if (hrNotes != null) {
            application.setHrNotes(hrNotes);
        }

        return applicationRepository.save(application);
    }

    // פונקציה למחיקת מועמדות לפי ID
    public void deleteApplication(Long applicationId) {
        // 1. נבדוק קודם אם המועמדות קיימת בכלל
        if (!applicationRepository.existsById(applicationId)) {
            throw new IllegalArgumentException("Application not found with id: " + applicationId);
        }

        // 2. מחיקה ממסד הנתונים
        applicationRepository.deleteById(applicationId);
    }
    // הוסיפי את הפונקציה הזו בתוך ApplicationService

    public List<ApplicationResponseDTO> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<ApplicationResponseDTO> getApplicationsByCandidate(Long candidateId) {
        var candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new IllegalArgumentException("Referenced user is not a candidate");
        }

        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ApplicationResponseDTO getApplicationById(Long applicationId) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));

        // TODO: Verify that this application belongs to the authenticated Candidate once authentication is implemented.
        return toResponseDTO(application);
    }

    public ApplicationResponseDTO submitTask(Long applicationId, String repositoryUrl) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));

        // TODO: Verify that this application belongs to the authenticated Candidate once authentication is implemented.
        if (application.getStatus() == ApplicationStatus.TASK_SUBMITTED) {
            throw new IllegalArgumentException("Technical task has already been submitted");
        }
        if (application.getStatus() != ApplicationStatus.TASK_SENT) {
            throw new IllegalArgumentException("Application status does not allow technical task submission");
        }
        if (application.getTaskDeadline() == null) {
            throw new IllegalArgumentException("Technical task has no assigned deadline");
        }

        LocalDateTime serverTime = LocalDateTime.now();
        if (!application.getTaskDeadline().isAfter(serverTime)) {
            throw new IllegalArgumentException("Technical task deadline has passed");
        }

        String normalizedRepositoryUrl = validateRepositoryUrl(repositoryUrl);
        application.setTaskRepoUrl(normalizedRepositoryUrl);
        application.setStatus(ApplicationStatus.TASK_SUBMITTED);

        return toResponseDTO(applicationRepository.save(application));
    }

    private String validateRepositoryUrl(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new IllegalArgumentException("Repository URL is required");
        }

        String trimmedUrl = repositoryUrl.trim();
        try {
            URI uri = new URI(trimmedUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            boolean validScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            boolean validHost = "github.com".equalsIgnoreCase(host)
                    || "www.github.com".equalsIgnoreCase(host);
            long pathSegments = Arrays.stream(uri.getPath().split("/"))
                    .filter(segment -> !segment.isBlank())
                    .count();

            if (!validScheme || !validHost || pathSegments < 2) {
                throw new IllegalArgumentException(
                        "Repository URL must be a valid GitHub repository URL in the form /owner/repository");
            }
            return trimmedUrl;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Repository URL is malformed");
        }
    }

    private ApplicationResponseDTO toResponseDTO(Application application) {
        var job = application.getJob();
        return new ApplicationResponseDTO(
                application.getId(),
                application.getCandidate().getId(),
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation(),
                application.getCoverLetter(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getTaskDeadline(),
                application.getTaskRepoUrl()
        );
    }
}
