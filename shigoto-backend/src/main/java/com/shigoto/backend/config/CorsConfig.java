package com.shigoto.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures cors behavior for the application.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Defines the browser origins, methods, headers, and credential policy permitted for API requests.
     * @param registry the MVC CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Permit the local React development origins to call every API route with session cookies.
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
