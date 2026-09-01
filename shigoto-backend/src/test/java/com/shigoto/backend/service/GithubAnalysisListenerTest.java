package com.shigoto.backend.service;

import com.shigoto.backend.messaging.GithubAnalysisRequestedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GithubAnalysisListenerTest {
    @Test
    void delegatesEventProcessing() {
        GithubAnalysisProcessor processor = mock(GithubAnalysisProcessor.class);
        GithubAnalysisListener listener = new GithubAnalysisListener(processor);
        GithubAnalysisRequestedEvent event = GithubAnalysisRequestedEvent.of(1L, 2L, "octocat");

        listener.receive(event);

        verify(processor).process(event);
    }
}
