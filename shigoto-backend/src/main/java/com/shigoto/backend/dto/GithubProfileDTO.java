package com.shigoto.backend.dto;

// שימי לב שהשמות חייבים להיות זהים לאיך ש-GitHub שולח אותם
public record GithubProfileDTO(
        String login,         // שם המשתמש
        String name,          // שם מלא
        int public_repos,     // כמות פרויקטים פומביים
        int followers,        // כמות עוקבים
        String html_url       // קישור ישיר לפרופיל
) {}