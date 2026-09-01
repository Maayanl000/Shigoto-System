package com.shigoto.backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GithubProfileUrlParserTest {
    @Test
    void extractsUsernameFromProfileUrls() {
        assertEquals("octocat", GithubProfileUrlParser.extractUsername("https://github.com/octocat").orElseThrow());
        assertEquals("octocat", GithubProfileUrlParser.extractUsername(" https://www.github.com/octocat/ ").orElseThrow());
    }

    @Test
    void rejectsRepositoriesWrongHostsAndMalformedUrls() {
        assertTrue(GithubProfileUrlParser.extractUsername("https://github.com/octocat/repo").isEmpty());
        assertTrue(GithubProfileUrlParser.extractUsername("https://example.com/octocat").isEmpty());
        assertTrue(GithubProfileUrlParser.extractUsername("https://github.com").isEmpty());
        assertTrue(GithubProfileUrlParser.extractUsername("not a url").isEmpty());
    }
}
