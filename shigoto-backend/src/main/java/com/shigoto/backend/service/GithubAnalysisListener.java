package com.shigoto.backend.service;

import com.shigoto.backend.messaging.GithubAnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

/**
 * Processes asynchronous GitHub analysis requests and maintains the candidate's analysis state.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@Service
@RequiredArgsConstructor
public class GithubAnalysisListener {
    private final GithubAnalysisProcessor processor;

    /**
     * Consumes an asynchronous event, validates its references, and performs the configured downstream work.
     * @param event the domain event to process
     */
    @JmsListener(destination = "shigoto.github-analysis")
    public void receive(GithubAnalysisRequestedEvent event) {
        processor.process(event);
    }
}
