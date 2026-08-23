package com.shigoto.backend.controller;

import com.shigoto.backend.dto.ApplicationResponseDTO;
import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus; // ה-import הנקי של הסטטוס
import com.shigoto.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // מעטפת נתונים (Record) ליצירת מועמדות חדשה
    public record ApplicationRequest(Long candidateId, Long jobId, String cvUrl, String coverLetter) {}

    // מעטפת נתונים (Record) חדשה לעדכון סטטוס והערות מנהל גיוס
    public record UpdateApplicationRequest(ApplicationStatus status, String hrNotes) {}

    @PostMapping
    public ResponseEntity<Application> createApplication(@RequestBody ApplicationRequest request) {
        if (request.candidateId() == null) {
            throw new IllegalArgumentException("candidateId must not be null");
        }
        if (request.jobId() == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }

        Application savedApplication = applicationService.createApplication(
                request.candidateId(),
                request.jobId(),
                request.cvUrl(),
                request.coverLetter()
        );

        return ResponseEntity.ok(savedApplication);
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponseDTO>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<List<ApplicationResponseDTO>> getApplicationsByCandidate(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(applicationService.getApplicationsByCandidate(candidateId));
    }

    // נקודת קצה (Endpoint) חדשה לעדכון מועמדות לפי ה-ID שלה
    @PutMapping("/{id}")
    public ResponseEntity<Application> updateApplication(
            @PathVariable Long id,
            @RequestBody UpdateApplicationRequest request) {

        Application updatedApplication = applicationService.updateApplicationStatus(
                id,
                request.status(),
                request.hrNotes()
        );

        return ResponseEntity.ok(updatedApplication);
    }
    // נקודת קצה למחיקת מועמדות לפי ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build(); // מחזיר סטטוס 24 No Content המציין שהמחיקה הצליחה
    }
}
