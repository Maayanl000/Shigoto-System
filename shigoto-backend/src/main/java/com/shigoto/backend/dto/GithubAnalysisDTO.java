package com.shigoto.backend.dto;

import com.shigoto.backend.entity.GithubAnalysisStatus;
import com.shigoto.backend.entity.GithubData;

import java.time.LocalDateTime;
import java.util.List;

public record GithubAnalysisDTO(
        GithubAnalysisStatus status,
        Integer publicRepositoryCount,
        List<String> topLanguages,
        LocalDateTime latestPushAt,
        LocalDateTime analyzedAt
) {
    public static GithubAnalysisDTO from(GithubData data) {
        if (data == null) return null;
        return new GithubAnalysisDTO(data.getStatus(), data.getPublicRepositoryCount(),
                data.getTopLanguages() == null ? List.of() : List.copyOf(data.getTopLanguages()),
                data.getLatestPushAt(), data.getAnalyzedAt());
    }
}
