package com.shigoto.backend;

import com.shigoto.backend.entity.*;
import com.shigoto.backend.repository.CompanyRepository;
import com.shigoto.backend.repository.JobRepository;
import com.shigoto.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootApplication
public class ShigotoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShigotoApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, JobRepository jobRepository,
                                   CompanyRepository companyRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            // 1. משיכת החברה קודם. רק אם היא לא קיימת - ניצור אותה
            Company wix;
            List<Company> companies = companyRepository.findAll();
            if (companies.isEmpty()) {
                wix = Company.builder()
                        .name("Wix")
                        .build();
                wix = companyRepository.save(wix); // שומרים רק בפעם הראשונה באמת
            } else {
                wix = companies.get(0); // החברה כבר קיימת, פשוט נשתמש בה כדי לקשר למשתמש ולמשרה
            }

            // 2. יצירת משתמש (השיוך לחברה הוא אופציונלי למשתמש, אבל נוסיף למען הסדר)
            User hrUser = User.builder()
                    .firstName("Maayan")
                    .lastName("Lahyani")
                    .email("maayan@wix.com")
                    .password(passwordEncoder.encode("password123")) // Development seed only
                    .role(Role.HR)
                    .company(wix) // שיוך המגייסת לחברה שמצאנו או יצרנו
                    .build();

            if (userRepository.findByEmail(hrUser.getEmail()).isEmpty()) {
                userRepository.save(hrUser);
            }

            // 3. יצירת משרה עם חובה לשייך חברה
            if (jobRepository.count() == 0) {
                Job javaJob = Job.builder()
                        .title("Java Backend Developer")
                        .description("Looking for a strong Java Spring Boot developer.")
                        .location("Tel Aviv")
                        .status(JobStatus.OPEN)
                        .company(wix) // שיוך המשרה לחברה
                        .build();
                jobRepository.save(javaJob);
            }
        };
    }
}
