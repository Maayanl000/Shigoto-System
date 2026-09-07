package com.shigoto.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Persists interview state and its domain relationships.
 */
@Entity
@Table(name = "interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    // Each interview belongs to one application and one company-scoped interviewer.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private String meetingLink;

    @Column(columnDefinition = "TEXT")
    private String feedback; // Submitted interviewer feedback used to complete the interview.

    @Column(columnDefinition = "TEXT")
    private String interviewerNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;
}
