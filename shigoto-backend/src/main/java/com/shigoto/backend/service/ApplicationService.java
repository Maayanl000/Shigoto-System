package com.shigoto.backend.service;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus; // הנה ה-import הנקי שהוספנו!
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with id: " + candidateId));

        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with id: " + jobId));

        List<Application> existingApplications = applicationRepository.findByCandidateId(candidateId);
        boolean alreadyApplied = existingApplications.stream()
                .anyMatch(app -> app.getJob().getId().equals(jobId));

        if (alreadyApplied) {
            throw new IllegalStateException("Candidate has already applied for this job!");
        }

        Application application = Application.builder()
                .candidate(candidate)
                .job(job)
                .cvUrl(cvUrl)
                .coverLetter(coverLetter)
                .build();

        return applicationRepository.save(application);
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
                .map(app -> new ApplicationResponseDTO(
                        app.getId(),
                        app.getCandidate().getId(), // משיכת ה-ID של המועמד
                        app.getJob().getId(),       // משיכת ה-ID של המשרה
                        app.getCoverLetter(),
                        app.getStatus(),
                        app.getAppliedAt()
                ))
                .toList();
    }
}