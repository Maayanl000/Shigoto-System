package com.shigoto.backend.repository;

import com.shigoto.backend.entity.GithubData;
import com.shigoto.backend.entity.GithubAnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GithubDataRepository extends JpaRepository<GithubData, Long> {
    Optional<GithubData> findByCandidateId(Long candidateId);
    boolean existsByLastEventIdAndStatusNot(UUID eventId, GithubAnalysisStatus status);
}
