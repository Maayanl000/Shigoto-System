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
