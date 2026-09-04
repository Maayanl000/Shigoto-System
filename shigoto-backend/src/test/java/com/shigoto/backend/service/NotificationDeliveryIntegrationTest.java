package com.shigoto.backend.service;

import com.shigoto.backend.entity.NotificationType;
import com.shigoto.backend.messaging.CandidateNotificationEvent;
import com.shigoto.backend.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

@SpringBootTest(properties = "shigoto.demo-data.enabled=false")
class NotificationDeliveryIntegrationTest {

    @Autowired private NotificationService notificationService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoSpyBean private NotificationRepository notificationRepository;

    private long companyId;
    private long candidateId;
    private long jobId;
    private long applicationId;
    private UUID eventId;

    @BeforeEach
    void createFixtureWithAlreadyPersistedEvent() {
        long base = -(System.nanoTime() & Long.MAX_VALUE);
        companyId = base;
        candidateId = base - 1;
        jobId = base - 2;
        applicationId = base - 3;
        eventId = UUID.randomUUID();
        jdbcTemplate.update("insert into companies (id, name) values (?, ?)", companyId, "JMS " + -base);
        jdbcTemplate.update("""
                insert into users (id, first_name, last_name, email, password, role, student)
                values (?, 'Jms', 'Candidate', ?, 'encoded', 'CANDIDATE', false)
                """, candidateId, "jms-" + -base + "@example.com");
        jdbcTemplate.update("insert into jobs (id, title, company_id, status) values (?, 'JMS Job', ?, 'OPEN')",
                jobId, companyId);
        jdbcTemplate.update("""
                insert into applications (id, candidate_id, job_id, status, version)
                values (?, ?, ?, 'APPLIED', 0)
                """, applicationId, candidateId, jobId);
        jdbcTemplate.update("""
                insert into notifications
                    (event_id, recipient_id, type, title, message, application_id, created_at)
                values (?, ?, 'APPLICATION_SUBMITTED', 'Existing', 'Existing', ?, now())
                """, eventId, candidateId, applicationId);
        doReturn(false).when(notificationRepository).existsByEventId(eventId);
    }

    @AfterEach
    void removeFixture() {
        jdbcTemplate.update("delete from notifications where event_id = ?", eventId);
        jdbcTemplate.update("delete from applications where id = ?", applicationId);
        jdbcTemplate.update("delete from jobs where id = ?", jobId);
        jdbcTemplate.update("delete from users where id = ?", candidateId);
        jdbcTemplate.update("delete from companies where id = ?", companyId);
    }

    @Test
    void duplicateConstraintRaceDoesNotEscapeAsRollbackOnlyFailure() {
        CandidateNotificationEvent duplicate = new CandidateNotificationEvent(
                eventId, NotificationType.APPLICATION_SUBMITTED, candidateId, applicationId,
                null, LocalDateTime.now());

        assertDoesNotThrow(() -> notificationService.receive(duplicate));
        assertEquals(1, notificationCount());
    }

    @Test
    void firstDeliveryPersistsExactlyOneNotification() {
        jdbcTemplate.update("delete from notifications where event_id = ?", eventId);
        CandidateNotificationEvent firstDelivery = new CandidateNotificationEvent(
                eventId, NotificationType.APPLICATION_SUBMITTED, candidateId, applicationId,
                null, LocalDateTime.now());

        assertDoesNotThrow(() -> notificationService.receive(firstDelivery));
        assertEquals(1, notificationCount());
    }

    private int notificationCount() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from notifications where event_id = ?", Integer.class, eventId);
        return count == null ? 0 : count;
    }
}
