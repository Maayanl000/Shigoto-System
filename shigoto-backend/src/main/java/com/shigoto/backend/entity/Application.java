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

    @Column(updatable = false)
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ApplicationStatus.APPLIED; // סטטוס ברירת מחדל
        }
    }
}
