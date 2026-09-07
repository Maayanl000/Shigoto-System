package com.shigoto.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the Shigoto Spring application.
 */
@SpringBootApplication
public class ShigotoApplication {

    /**
     * Starts the Spring Boot application.
     * @param args application startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ShigotoApplication.class, args);
    }
}
