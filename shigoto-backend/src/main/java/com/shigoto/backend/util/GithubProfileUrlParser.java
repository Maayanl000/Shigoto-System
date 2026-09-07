package com.shigoto.backend.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parses supported GitHub profile URLs without retaining state.
 */
public final class GithubProfileUrlParser {
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*");

    /**
     * Prevents instantiation of this stateless URL utility.
     */
    private GithubProfileUrlParser() {}

    /**
     * Extracts a valid single-segment GitHub username from an HTTP(S) profile URL.
     * @param value the value to validate or normalize
     * @return the GitHub username when the URL is a valid single-segment profile URL; otherwise empty
     */
    public static Optional<String> extractUsername(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            URI uri = new URI(value.trim());
            boolean validScheme = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            boolean validHost = "github.com".equalsIgnoreCase(uri.getHost())
                    || "www.github.com".equalsIgnoreCase(uri.getHost());
            if (!validScheme || !validHost || uri.getUserInfo() != null || uri.getPort() != -1) {
                return Optional.empty();
            }

            String[] segments = java.util.Arrays.stream(uri.getPath().split("/"))
                    .filter(segment -> !segment.isBlank())
                    .toArray(String[]::new);
            if (segments.length != 1 || segments[0].length() > 39
                    || !USERNAME_PATTERN.matcher(segments[0]).matches()) {
                return Optional.empty();
            }
            return Optional.of(segments[0]);
        } catch (URISyntaxException ex) {
            return Optional.empty();
        }
    }
}
