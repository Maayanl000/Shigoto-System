package com.shigoto.backend.repository;

import com.shigoto.backend.entity.GithubData;
import com.shigoto.backend.entity.GithubAnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Declares application-specific persistence queries for candidate GitHub analysis data.
 */
public interface GithubDataRepository extends JpaRepository<GithubData, Long> {
    /**
     * Finds the stored GitHub analysis associated with a candidate.
     * @param candidateId the candidate identifier
     * @return the candidate's GitHub analysis, if present
     */
    Optional<GithubData> findByCandidateId(Long candidateId);
    /**
     * Checks whether an analysis event has already reached a status other than the supplied status.
     * @param eventId the event id
     * @param status the requested domain status
     * @return {@code true} when the event was recorded with a different status; otherwise {@code false}
     */
    boolean existsByLastEventIdAndStatusNot(UUID eventId, GithubAnalysisStatus status);
}
