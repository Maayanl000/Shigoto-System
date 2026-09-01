package com.shigoto.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "github_data", uniqueConstraints =
        @UniqueConstraint(name = "uk_github_data_candidate", columnNames = "candidate_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private User candidate;

    @Column(nullable = false, length = 39)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GithubAnalysisStatus status;

    private Integer publicRepositoryCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "github_data_top_languages",
            joinColumns = @JoinColumn(name = "github_data_id"))
    @OrderColumn(name = "language_rank")
    @Column(name = "language", nullable = false, length = 100)
    @Builder.Default
    private List<String> topLanguages = new ArrayList<>();

    private LocalDateTime latestPushAt;
    private LocalDateTime analyzedAt;

    @Column(nullable = false)
    private UUID lastEventId;
}
