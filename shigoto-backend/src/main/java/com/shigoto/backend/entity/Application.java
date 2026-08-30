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

    // ה"גשר" הראשון - הקישור ללקוח (המועמד)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    // ה"גשר" השני - הקישור למנה (המשרה)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    // Internal UUID storage key (not a public URL or filesystem path).
    private String cvUrl;

    @Column(columnDefinition = "TEXT")
    private String coverLetter; // מכתב מקדים

    @Column(columnDefinition = "TEXT")
    private String hrNotes; // הערות מנהלת הגיוס

    private LocalDateTime taskDeadline; // תאריך יעד למבחן בית

    @Column(columnDefinition = "TEXT")
    private String taskInstructions;

    private String taskRepoUrl; // קישור לגיטהאב של הפתרון

    @Column(columnDefinition = "TEXT")
    private String taskReviewNotes;

    @Column(columnDefinition = "TEXT")
    private String candidateFeedback;

    @Column(updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime statusChangedAt;

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
            this.status = ApplicationStatus.APPLIED; // סטטוס ברירת מחדל
        }
    }

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
