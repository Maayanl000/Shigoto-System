package com.shigoto.backend.service;

import com.shigoto.backend.messaging.GithubAnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GithubAnalysisListener {
    private final GithubAnalysisProcessor processor;

    @JmsListener(destination = "shigoto.github-analysis")
    public void receive(GithubAnalysisRequestedEvent event) {
        processor.process(event);
    }
}
