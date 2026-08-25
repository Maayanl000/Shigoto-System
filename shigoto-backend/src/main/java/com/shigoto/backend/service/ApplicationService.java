package com.shigoto.backend.service;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.dto.StaffApplicationResponseDTO;
import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus; // הנה ה-import הנקי שהוספנו!
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.DuplicateApplicationException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CvStorageService cvStorageService;

    @Transactional
    public ApplicationResponseDTO createApplication(User candidate, Long jobId, String coverLetter, MultipartFile cv) {
        if (candidate.getRole() != Role.CANDIDATE) {
            throw new IllegalArgumentException("Referenced user is not a candidate");
        }

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalArgumentException("Job is not open for applications");
        }

        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getId(), jobId)) {
            throw new DuplicateApplicationException("Candidate has already applied for this job");
        }

        String storageKey = cvStorageService.store(cv);
        try {
            Application application = Application.builder()
                    .candidate(candidate)
                    .job(job)
                    .cvUrl(storageKey)
                    .coverLetter(coverLetter)
                    .build();
            return toResponseDTO(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException ex) {
            deleteStoredCvAfterFailedApplication(storageKey);
            throw new DuplicateApplicationException("Candidate has already applied for this job");
        } catch (RuntimeException ex) {
            deleteStoredCvAfterFailedApplication(storageKey);
            throw ex;
        }
    }


    // פונקציית העדכון המעודכנת והנקייה שלנו
    public StaffApplicationResponseDTO updateApplicationStatus(
            Long applicationId, ApplicationStatus newStatus, String hrNotes) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found with id: " + applicationId));

        if (newStatus != null) {
            application.setStatus(newStatus);
        }
        if (hrNotes != null) {
            application.setHrNotes(hrNotes);
        }

        return StaffApplicationResponseDTO.from(applicationRepository.save(application));
    }

    // פונקציה למחיקת מועמדות לפי ID
    public void deleteApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found with id: " + applicationId));
        applicationRepository.delete(application);
        applicationRepository.flush();
        cvStorageService.delete(application.getCvUrl());
    }
    // הוסיפי את הפונקציה הזו בתוך ApplicationService

    public List<StaffApplicationResponseDTO> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(StaffApplicationResponseDTO::from)
                .toList();
    }

    public List<StaffApplicationResponseDTO> getApplicationsByCandidate(Long candidateId) {
        var candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new IllegalArgumentException("Referenced user is not a candidate");
        }

        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId)
                .stream()
                .map(StaffApplicationResponseDTO::from)
                .toList();
    }

    public List<ApplicationResponseDTO> getApplicationsForCandidate(User candidate) {
        requireCandidateRole(candidate);
        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidate.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ApplicationResponseDTO getOwnedApplicationById(Long applicationId, User candidate) {
        return toResponseDTO(findOwnedApplication(applicationId, candidate));
    }

    public CvDownload getOwnedCv(Long applicationId, User candidate) {
        Application application = findOwnedApplication(applicationId, candidate);
        Resource resource = cvStorageService.load(application.getCvUrl());
        return new CvDownload("cv-application-" + applicationId + ".pdf", resource);
    }

    public ApplicationResponseDTO submitTask(Long applicationId, String repositoryUrl, User candidate) {
        Application application = findOwnedApplication(applicationId, candidate);
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

    private Application findOwnedApplication(Long applicationId, User candidate) {
        requireCandidateRole(candidate);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));
        if (!Objects.equals(application.getCandidate().getId(), candidate.getId())) {
            throw new AccessDeniedException("Application does not belong to the authenticated candidate");
        }
        return application;
    }

    private void requireCandidateRole(User candidate) {
        if (candidate == null || candidate.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Candidate access is required");
        }
    }

    private void deleteStoredCvAfterFailedApplication(String storageKey) {
        try {
            cvStorageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // Preserve the original database error; storage cleanup can be investigated separately.
        }
    }

    public record CvDownload(String downloadFilename, Resource resource) {}

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
