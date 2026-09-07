package com.shigoto.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_candidate_job",
                columnNames = {"candidate_id", "job_id"}
        )
)
/**
 * Persists application state and its domain relationships.
 * Lombok generates the no-argument constructor required by JPA and an all-fields constructor used by builder-backed construction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    // The candidate and job links define ownership and company scope for this application.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    // Internal UUID storage key (not a public URL or filesystem path).
    private String cvUrl;

    @Column(columnDefinition = "TEXT")
    private String coverLetter; // Candidate-provided cover letter.

    @Column(columnDefinition = "TEXT")
    private String hrNotes; // Internal HR notes, never candidate-facing.

    private LocalDateTime taskDeadline; // Deadline for the assigned home task.

    @Column(columnDefinition = "TEXT")
    private String taskInstructions;

    private String taskRepoUrl; // GitHub repository submitted as the task solution.

    @Column(columnDefinition = "TEXT")
    private String taskReviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_reviewer_id")
    private User taskReviewer;

    @Column(columnDefinition = "TEXT")
    private String candidateFeedback;

    @Column(updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime statusChangedAt;

    /**
     * Initializes persistence defaults immediately before the entity is first stored.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime createdAt = LocalDateTime.now();
        if (this.appliedAt == null) {
            this.appliedAt = createdAt;
        }
        if (this.statusChangedAt == null) {
            this.statusChangedAt = this.appliedAt;
        }
        if (this.status == null) {
            this.status = ApplicationStatus.APPLIED; // New applications begin at the submitted stage.
        }
    }

    /**
     * Moves the application to the requested workflow status and records the transition time.
     * @param newStatus the new status
     */
    public void transitionTo(ApplicationStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Application status is required");
        }
        if (newStatus != this.status) {
            this.status = newStatus;
            this.statusChangedAt = LocalDateTime.now();
        }
    }
}
