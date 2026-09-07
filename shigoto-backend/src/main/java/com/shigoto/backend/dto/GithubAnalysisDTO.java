package com.shigoto.backend.dto;

import com.shigoto.backend.entity.GithubAnalysisStatus;
import com.shigoto.backend.entity.GithubData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents public GitHub activity metrics exposed for a candidate.
 */
public record GithubAnalysisDTO(
        GithubAnalysisStatus status,
        Integer publicRepositoryCount,
        List<String> topLanguages,
        LocalDateTime latestPushAt,
        LocalDateTime analyzedAt
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param data the data
     * @return a DTO populated from the supplied domain entity
     */
    public static GithubAnalysisDTO from(GithubData data) {
        if (data == null) return null;
        return new GithubAnalysisDTO(data.getStatus(), data.getPublicRepositoryCount(),
                data.getTopLanguages() == null ? List.of() : List.copyOf(data.getTopLanguages()),
                data.getLatestPushAt(), data.getAnalyzedAt());
    }
}
