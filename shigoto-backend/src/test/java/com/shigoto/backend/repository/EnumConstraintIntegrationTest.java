package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.entity.Notification;
import com.shigoto.backend.entity.NotificationType;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "shigoto.demo-data.enabled=false")
@Transactional
class EnumConstraintIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void everyApplicationStatusCanBePersisted() {
        long unique = System.nanoTime();
        Company company = Company.builder().name("Enum Constraint Company " + unique).build();
        entityManager.persist(company);
        Job job = Job.builder().title("Enum Constraint Job").company(company).status(JobStatus.OPEN).build();
        entityManager.persist(job);

        for (ApplicationStatus status : ApplicationStatus.values()) {
            User candidate = candidate("application-status-" + status + "-" + unique + "@example.test");
            entityManager.persist(candidate);
            Application application = Application.builder()
                    .candidate(candidate)
                    .job(job)
                    .status(status)
                    .build();
            entityManager.persist(application);
            entityManager.flush();
            assertNotNull(application.getId());
        }
    }

    @Test
    void everyNotificationTypeCanBePersisted() {
        long unique = System.nanoTime();
        User candidate = candidate("notification-type-" + unique + "@example.test");
        entityManager.persist(candidate);

        for (NotificationType type : NotificationType.values()) {
            Notification notification = Notification.builder()
                    .eventId(UUID.randomUUID())
                    .recipient(candidate)
                    .type(type)
                    .title("Enum constraint test")
                    .message("Enum constraint test")
                    .build();
            entityManager.persist(notification);
            entityManager.flush();
            assertNotNull(notification.getId());
        }
    }

    @Test
    void applicationSubmittedPostgresqlConstraintPatchIsTracked() throws Exception {
        String patch = Files.readString(Path.of(
                "database", "patches", "2026-09-01-application-submitted-notification.sql"));

        assertTrue(patch.contains("'APPLICATION_SUBMITTED'"));
        assertTrue(patch.contains("DROP CONSTRAINT IF EXISTS notifications_type_check"));
        assertTrue(patch.contains("ADD CONSTRAINT notifications_type_check"));
    }

    private User candidate(String email) {
        return User.builder()
                .firstName("Enum")
                .lastName("Constraint")
                .email(email)
                .password("not-used")
                .role(Role.CANDIDATE)
                .build();
    }
}
