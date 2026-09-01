package com.shigoto.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubServiceTest {
    @Test
    void aggregatesLanguageBytesAndReturnsEveryLanguageInDeterministicOrder() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubService service = new GithubService(builder, "https://api.github.test", "secret-token");
        server.expect(once(), requestTo("https://api.github.test/users/octocat"))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withSuccess("{\"public_repos\":7}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(
                        "https://api.github.test/users/octocat/repos?type=owner&sort=pushed&direction=desc&per_page=100"))
                .andRespond(withSuccess("""
                        [
                          {"name":"older","fork":false,"archived":false,"pushed_at":"2026-08-01T10:00:00Z"},
                          {"name":"newest","fork":false,"archived":false,"pushed_at":"2026-08-03T10:00:00Z"},
                          {"name":"middle","fork":false,"archived":false,"pushed_at":"2026-08-02T10:00:00Z"},
                          {"name":"forked","fork":true,"archived":false,"pushed_at":"2026-09-01T10:00:00Z"},
                          {"name":"archived","fork":false,"archived":true,"pushed_at":"2026-09-02T10:00:00Z"}
                        ]
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.test/repos/octocat/newest/languages"))
                .andRespond(withSuccess("{\"Python\":200,\"Java\":20,\"Shell\":50}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.test/repos/octocat/middle/languages"))
                .andRespond(withSuccess("{\"Ruby\":300,\"Go\":50}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.github.test/repos/octocat/older/languages"))
                .andRespond(withSuccess("{\"Java\":100,\"Ruby\":40}", MediaType.APPLICATION_JSON));

        GithubService.GithubAnalysisResult result = service.analyze("octocat");

        assertEquals(7, result.publicRepositoryCount());
        assertEquals(List.of("Ruby", "Python", "Java", "Go", "Shell"), result.topLanguages());
        assertEquals(LocalDateTime.of(2026, 8, 3, 10, 0), result.latestPushAt());
        server.verify();
    }

    @Test
    void omitsAuthorizationHeaderWhenTokenIsBlank() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubService service = new GithubService(builder, "https://api.github.test", "  ");
        server.expect(requestTo("https://api.github.test/users/octocat"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("{\"public_repos\":0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.test/users/octocat/repos?type=owner&sort=pushed&direction=desc&per_page=100"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(0, service.analyze("octocat").publicRepositoryCount());
        server.verify();
    }

    @Test
    void analyzesAtMostTwentyMostRecentlyPushedEligibleRepositories() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubService service = new GithubService(builder, "https://api.github.test", "");
        server.expect(requestTo("https://api.github.test/users/octocat"))
                .andRespond(withSuccess("{\"public_repos\":21}", MediaType.APPLICATION_JSON));
        String repositories = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> "{\"name\":\"repo-%02d\",\"fork\":false,\"archived\":false,".formatted(index)
                        + "\"pushed_at\":\"2026-08-%02dT10:00:00Z\"}".formatted(index))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        server.expect(requestTo(
                        "https://api.github.test/users/octocat/repos?type=owner&sort=pushed&direction=desc&per_page=100"))
                .andRespond(withSuccess(repositories, MediaType.APPLICATION_JSON));
        IntStream.iterate(21, index -> index - 1).limit(20).forEach(index ->
                server.expect(requestTo("https://api.github.test/repos/octocat/repo-%02d/languages".formatted(index)))
                        .andRespond(withSuccess("{\"Java\":1}", MediaType.APPLICATION_JSON)));

        GithubService.GithubAnalysisResult result = service.analyze("octocat");

        assertEquals(List.of("Java"), result.topLanguages());
        assertEquals(LocalDateTime.of(2026, 8, 21, 10, 0), result.latestPushAt());
        server.verify();
    }

    @Test
    void skipsOneFailedLanguageEndpointAndKeepsSuccessfulLanguages() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubService service = new GithubService(builder, "https://api.github.test", "");
        server.expect(requestTo("https://api.github.test/users/octocat"))
                .andRespond(withSuccess("{\"public_repos\":2}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.github.test/users/octocat/repos?type=owner&sort=pushed&direction=desc&per_page=100"))
                .andRespond(withSuccess("""
                        [
                          {"name":"broken","fork":false,"archived":false,"pushed_at":"2026-08-02T10:00:00Z"},
                          {"name":"working","fork":false,"archived":false,"pushed_at":"2026-08-01T10:00:00Z"}
                        ]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.test/repos/octocat/broken/languages"))
                .andRespond(withServerError());
        server.expect(requestTo("https://api.github.test/repos/octocat/working/languages"))
                .andRespond(withSuccess("{\"Java\":500,\"Shell\":25}", MediaType.APPLICATION_JSON));

        GithubService.GithubAnalysisResult result = service.analyze("octocat");

        assertEquals(2, result.publicRepositoryCount());
        assertEquals(List.of("Java", "Shell"), result.topLanguages());
        assertEquals(LocalDateTime.of(2026, 8, 2, 10, 0), result.latestPushAt());
        server.verify();
    }

    @Test
    void skipsSeveralFailedLanguageEndpointsWhenOneSucceeds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubService service = new GithubService(builder, "https://api.github.test", "");
        server.expect(requestTo("https://api.github.test/users/octocat"))
                .andRespond(withSuccess("{\"public_repos\":3}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.github.test/users/octocat/repos?type=owner&sort=pushed&direction=desc&per_page=100"))
                .andRespond(withSuccess("""
                        [
                          {"name":"broken-one","fork":false,"archived":false,"pushed_at":"2026-08-03T10:00:00Z"},
                          {"name":"broken-two","fork":false,"archived":false,"pushed_at":"2026-08-02T10:00:00Z"},
                          {"name":"working","fork":false,"archived":false,"pushed_at":"2026-08-01T10:00:00Z"}
                        ]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.test/repos/octocat/broken-one/languages"))
                .andRespond(withServerError());
        server.expect(requestTo("https://api.github.test/repos/octocat/broken-two/languages"))
                .andRespond(withServerError());
        server.expect(requestTo("https://api.github.test/repos/octocat/working/languages"))
                .andRespond(withSuccess("{\"Python\":400}", MediaType.APPLICATION_JSON));

        GithubService.GithubAnalysisResult result = service.analyze("octocat");

        assertEquals(List.of("Python"), result.topLanguages());
        assertEquals(3, result.publicRepositoryCount());
        server.verify();
    }

    @Test
    void propagatesUserRequestFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubService service = new GithubService(builder, "https://api.github.test", "");
        server.expect(requestTo("https://api.github.test/users/octocat"))
                .andRespond(withServerError());

        assertThrows(RestClientResponseException.class, () -> service.analyze("octocat"));
        server.verify();
    }

    @Test
    void propagatesRepositoryListingFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GithubService service = new GithubService(builder, "https://api.github.test", "");
        server.expect(requestTo("https://api.github.test/users/octocat"))
                .andRespond(withSuccess("{\"public_repos\":1}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.github.test/users/octocat/repos?type=owner&sort=pushed&direction=desc&per_page=100"))
                .andRespond(withServerError());

        assertThrows(RestClientResponseException.class, () -> service.analyze("octocat"));
        server.verify();
    }
}
