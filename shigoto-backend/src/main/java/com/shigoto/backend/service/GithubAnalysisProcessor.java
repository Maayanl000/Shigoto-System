package com.shigoto.backend.service;

import com.shigoto.backend.entity.*;
import com.shigoto.backend.messaging.GithubAnalysisRequestedEvent;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.GithubDataRepository;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.util.GithubProfileUrlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubAnalysisProcessor {
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final GithubDataRepository githubDataRepository;
    private final GithubService githubService;

    @Transactional
    public void process(GithubAnalysisRequestedEvent event) {
        if (event == null || event.eventId() == null || event.candidateUserId() == null
                || event.applicationId() == null || event.githubUsername() == null
                || event.githubUsername().isBlank()) {
            log.warn("Ignoring malformed GitHub analysis event");
            return;
        }
        if (githubDataRepository.existsByLastEventIdAndStatusNot(
                event.eventId(), GithubAnalysisStatus.PENDING)) return;

        User candidate = userRepository.findById(event.candidateUserId()).orElse(null);
        Application application = applicationRepository.findById(event.applicationId()).orElse(null);
        if (candidate == null || candidate.getRole() != Role.CANDIDATE || application == null
                || application.getCandidate() == null
                || !Objects.equals(application.getCandidate().getId(), candidate.getId())) {
            log.warn("Ignoring GitHub analysis event {} with missing or mismatched references", event.eventId());
            return;
        }
        String currentUsername = GithubProfileUrlParser.extractUsername(candidate.getGithubProfileUrl())
                .orElse(null);
        if (currentUsername == null || !currentUsername.equalsIgnoreCase(event.githubUsername())) {
            log.info("Ignoring stale GitHub analysis event {}", event.eventId());
            return;
        }

        GithubData data = githubDataRepository.findByCandidateId(candidate.getId()).orElse(null);
        if (data != null && data.getUsername().equalsIgnoreCase(currentUsername)
                && (data.getStatus() == GithubAnalysisStatus.READY
                || data.getStatus() == GithubAnalysisStatus.NOT_FOUND)) {
            data.setLastEventId(event.eventId());
            githubDataRepository.save(data);
            return;
        }
        if (data == null) {
            data = GithubData.builder().candidate(candidate).username(currentUsername)
                    .status(GithubAnalysisStatus.PENDING).lastEventId(event.eventId()).build();
            candidate.setGithubData(data);
        } else {
            data.setUsername(currentUsername);
            data.setStatus(GithubAnalysisStatus.PENDING);
            data.setLastEventId(event.eventId());
            clearAnalysis(data);
        }

        try {
            GithubService.GithubAnalysisResult result = githubService.analyze(currentUsername);
            data.setStatus(GithubAnalysisStatus.READY);
            data.setPublicRepositoryCount(result.publicRepositoryCount());
            data.setTopLanguages(new ArrayList<>(result.topLanguages()));
            data.setLatestPushAt(result.latestPushAt());
        } catch (RestClientResponseException responseFailure) {
            data.setStatus(responseFailure.getStatusCode().value() == 404
                    ? GithubAnalysisStatus.NOT_FOUND : GithubAnalysisStatus.FAILED);
            clearAnalysis(data);
            log.warn("GitHub analysis request failed for event {} with status {}",
                    event.eventId(), responseFailure.getStatusCode().value());
        } catch (RestClientException requestFailure) {
            data.setStatus(GithubAnalysisStatus.FAILED);
            clearAnalysis(data);
            log.warn("GitHub analysis request failed for event {}", event.eventId());
        }
        data.setAnalyzedAt(LocalDateTime.now());
        githubDataRepository.save(data);
    }

    private void clearAnalysis(GithubData data) {
        data.setPublicRepositoryCount(null);
        data.setTopLanguages(new ArrayList<>());
        data.setLatestPushAt(null);
        data.setAnalyzedAt(null);
    }
}
