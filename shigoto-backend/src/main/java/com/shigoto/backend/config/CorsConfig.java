package com.shigoto.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // מאפשר גישה לכל נקודות הקצה שמתחילות ב-/api/
                //שתי הכוכביות (**) הן Wildcard שאומר "כל מה שבא אחר כך". כך זה יתפוס נתיבים כמו /api/applications או /api/users.
                .allowedOrigins("http://localhost:3000", "http://localhost:5173") // כתובות ה-React השכיחות (Vite לרוב משתמש ב-5173)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // הפעולות המותרות
                .allowedHeaders("*") // מאפשר את כל ה-Headers
                .allowCredentials(true); // מאפשר העברת Cookies (חשוב לעתיד אם נוסיף התחברות)
    }
}