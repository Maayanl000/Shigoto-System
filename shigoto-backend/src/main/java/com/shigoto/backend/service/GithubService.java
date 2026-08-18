package com.shigoto.backend.service;

import com.shigoto.backend.dto.GithubProfileDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GithubService {

    private final RestClient restClient;

    // בניית הלקוח עם כתובת הבסיס של גיטהאב
    public GithubService() {
        this.restClient = RestClient.create("https://api.github.com");
    }

    // הפונקציה ששולפת את הנתונים
    public GithubProfileDTO getCandidateProfile(String username) {
        return restClient.get()
                .uri("/users/{username}", username) // מכניס את שם המשתמש לכתובת
                .retrieve() // מבצע את הבקשה
                .body(GithubProfileDTO.class); // ממיר את ה-JSON של גיטהאב ל-DTO שלנו
    }
}