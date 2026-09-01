package com.shigoto.backend.service;

import com.shigoto.backend.entity.*;
import com.shigoto.backend.messaging.GithubAnalysisRequestedEvent;
import com.shigoto.backend.repository.ApplicationRepository;
import com.shigoto.backend.repository.GithubDataRepository;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GithubAnalysisProcessorTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final GithubDataRepository githubData = mock(GithubDataRepository.class);
    private final GithubService githubService = mock(GithubService.class);
    private final GithubAnalysisProcessor processor =
            new GithubAnalysisProcessor(users, applications, githubData, githubService);
    private User candidate;
    private Application application;
    private GithubAnalysisRequestedEvent event;

    @BeforeEach
    void setUp() {
        candidate = User.builder().id(1L).role(Role.CANDIDATE)
                .githubProfileUrl("https://github.com/octocat").build();
        application = Application.builder().id(2L).candidate(candidate).build();
        event = GithubAnalysisRequestedEvent.of(1L, 2L, "octocat");
        when(users.findById(1L)).thenReturn(Optional.of(candidate));
        when(applications.findById(2L)).thenReturn(Optional.of(application));
        when(githubData.findByCandidateId(1L)).thenReturn(Optional.empty());
    }

    @Test
    void analyzesAndPersistsCandidateLevelData() {
        when(githubService.analyze("octocat")).thenReturn(new GithubService.GithubAnalysisResult(
                5, List.of("Java", "TypeScript", "Python", "Go", "Shell"),
                LocalDateTime.of(2026, 8, 1, 12, 0)));

        processor.process(event);

        ArgumentCaptor<GithubData> saved = ArgumentCaptor.forClass(GithubData.class);
        verify(githubData).save(saved.capture());
        assertSame(candidate, saved.getValue().getCandidate());
        assertEquals(GithubAnalysisStatus.READY, saved.getValue().getStatus());
        assertEquals(5, saved.getValue().getPublicRepositoryCount());
        assertEquals(List.of("Java", "TypeScript", "Python", "Go", "Shell"),
                saved.getValue().getTopLanguages());
        assertEquals(event.eventId(), saved.getValue().getLastEventId());
        assertNotNull(saved.getValue().getAnalyzedAt());
    }

    @Test
    void duplicateEventDoesNothing() {
        when(githubData.existsByLastEventIdAndStatusNot(
                event.eventId(), GithubAnalysisStatus.PENDING)).thenReturn(true);

        processor.process(event);

        verifyNoInteractions(users, applications, githubService);
        verify(githubData, never()).save(any());
    }

    @Test
    void notFoundIsPersistedWithoutThrowing() {
        when(githubService.analyze("octocat")).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> processor.process(event));

        ArgumentCaptor<GithubData> saved = ArgumentCaptor.forClass(GithubData.class);
        verify(githubData).save(saved.capture());
        assertEquals(GithubAnalysisStatus.NOT_FOUND, saved.getValue().getStatus());
        assertNull(saved.getValue().getPublicRepositoryCount());
    }

    @Test
    void networkFailureIsPersistedWithoutThrowing() {
        when(githubService.analyze("octocat")).thenThrow(new ResourceAccessException("connection failed"));

        assertDoesNotThrow(() -> processor.process(event));

        ArgumentCaptor<GithubData> saved = ArgumentCaptor.forClass(GithubData.class);
        verify(githubData).save(saved.capture());
        assertEquals(GithubAnalysisStatus.FAILED, saved.getValue().getStatus());
        assertNotNull(saved.getValue().getAnalyzedAt());
    }

    @Test
    void updatesExistingCandidateRecordInsteadOfCreatingAnother() {
        GithubData existing = GithubData.builder().id(9L).candidate(candidate).username("octocat")
                .status(GithubAnalysisStatus.FAILED).lastEventId(java.util.UUID.randomUUID()).build();
        candidate.setGithubData(existing);
        when(githubData.findByCandidateId(1L)).thenReturn(Optional.of(existing));
        when(githubService.analyze("octocat")).thenReturn(
                new GithubService.GithubAnalysisResult(1, List.of("Java"), null));

        processor.process(event);

        verify(githubData).save(existing);
        assertEquals(GithubAnalysisStatus.READY, existing.getStatus());
        assertEquals(event.eventId(), existing.getLastEventId());
    }

    @Test
    void ignoresEventWhenCandidateProfileHasChanged() {
        candidate.setGithubProfileUrl("https://github.com/someone-else");

        processor.process(event);

        verifyNoInteractions(githubService);
        verify(githubData, never()).save(any());
    }
}
