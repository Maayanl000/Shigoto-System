package com.shigoto.backend.dto;

public record CandidateProfileUpdateRequestDTO(
        String firstName,
        String lastName,
        String githubProfileUrl
) {}
