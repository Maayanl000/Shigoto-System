package com.shigoto.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Queries public GitHub profile and repository data to summarize a candidate's development activity.
 */
@Service
@Slf4j
public class GithubService {

    private static final int MAX_REPOSITORIES_TO_ANALYZE = 20;

    private final RestClient restClient;

    /**
     * Creates a GitHub API client from the configured base URL and optional access token.
     * @param baseUrl the base url
     * @param token the token
     */
    @Autowired
    public GithubService(
            @Value("${shigoto.github.base-url:https://api.github.com}") String baseUrl,
            @Value("${shigoto.github.token:}") String token) {
        this(RestClient.builder(), baseUrl, token);
    }

    /**
     * Creates a GitHub client around the supplied builder for production use and focused tests.
     * @param restClientBuilder the Spring REST client builder
     * @param baseUrl the GitHub API base URL
     * @param token the optional bearer token used for authenticated rate limits
     */
    GithubService(RestClient.Builder restClientBuilder, String baseUrl, String token) {
        RestClient.Builder builder = restClientBuilder.baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json");
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token.trim());
        }
        this.restClient = builder.build();
    }

    /**
     * Fetches public GitHub repositories and summarizes languages and recent activity.
     * @param username the username
     * @return aggregate public-repository count, ranked languages, and latest push time
     */
    public GithubAnalysisResult analyze(String username) {
        // Fetch profile metadata and repositories before aggregating repository language bytes.
        GithubUserResponse user = restClient.get()
                .uri("/users/{username}", username)
                .retrieve()
                .body(GithubUserResponse.class);
        if (user == null) throw new org.springframework.web.client.RestClientException(
                "GitHub returned an empty user response");
        GithubRepositoryResponse[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/users/{username}/repos")
                        .queryParam("type", "owner")
                        .queryParam("sort", "pushed")
                        .queryParam("direction", "desc")
                        .queryParam("per_page", 100)
                        .build(username))
                .retrieve()
                .body(GithubRepositoryResponse[].class);

        // Ignore forks and archived repositories before querying per-repository language data.
        List<GithubRepositoryResponse> repositories = response == null ? List.of() : Arrays.stream(response)
                .filter(repository -> !repository.fork() && !repository.archived())
                .toList();
        Map<String, Long> languageCounts = new java.util.HashMap<>();
        repositories.stream()
                .filter(repository -> repository.name() != null && !repository.name().isBlank())
                .sorted(Comparator.comparing(GithubRepositoryResponse::pushedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(GithubRepositoryResponse::name))
                .limit(MAX_REPOSITORIES_TO_ANALYZE)
                .forEach(repository -> fetchLanguages(username, repository.name())
                        .forEach((language, bytes) -> {
                            if (language != null && !language.isBlank() && bytes != null && bytes > 0) {
                                languageCounts.merge(language, bytes, Long::sum);
                            }
                        }));
        // Rank languages deterministically by byte count and name.
        List<String> topLanguages = languageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
        LocalDateTime latestPushAt = repositories.stream()
                .map(GithubRepositoryResponse::pushedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(OffsetDateTime::toLocalDateTime)
                .orElse(null);
        return new GithubAnalysisResult(user.publicRepositoryCount(), topLanguages, latestPushAt);
    }

    /**
     * Fetches repository language byte counts, returning an empty map when GitHub cannot provide them.
     * @param owner the owner
     * @param repository the repository
     * @return language names mapped to reported byte counts, or an empty map after a request failure
     */
    private Map<String, Long> fetchLanguages(String owner, String repository) {
        try {
            Map<String, Long> languages = restClient.get()
                    .uri("/repos/{owner}/{repository}/languages", owner, repository)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return languages == null ? Map.of() : languages;
        } catch (RestClientException requestFailure) {
            log.warn("Skipping GitHub language analysis for repository {}/{} after API request failed",
                    owner, repository);
            return Map.of();
        }
    }

    /**
     * Carries the aggregate GitHub metrics returned to the analysis processor.
     * @param publicRepositoryCount the public repository count
     * @param topLanguages the top languages
     * @param latestPushAt the latest push at
     */
    public record GithubAnalysisResult(
            int publicRepositoryCount, List<String> topLanguages, LocalDateTime latestPushAt) {}

    /**
     * Maps the public repository count returned by the GitHub user endpoint.
     * @param publicRepositoryCount the public repository count
     */
    record GithubUserResponse(@JsonProperty("public_repos") int publicRepositoryCount) {}

    /**
     * Maps repository metadata used to filter and rank recent activity.
     * @param fork the fork
     * @param archived the archived
     * @param name the name to look up
     * @param pushedAt the pushed at
     */
    record GithubRepositoryResponse(
            boolean fork,
            boolean archived,
            String name,
            @JsonProperty("pushed_at") OffsetDateTime pushedAt) {}
}
