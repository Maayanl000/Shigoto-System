package com.shigoto.backend.controller;

import com.shigoto.backend.dto.GithubProfileDTO;
import com.shigoto.backend.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubController {

    private final GithubService githubService;

    @GetMapping("/{username}")
    public ResponseEntity<GithubProfileDTO> getGithubData(@PathVariable String username) {
        GithubProfileDTO profile = githubService.getCandidateProfile(username);
        return ResponseEntity.ok(profile);
    }
}