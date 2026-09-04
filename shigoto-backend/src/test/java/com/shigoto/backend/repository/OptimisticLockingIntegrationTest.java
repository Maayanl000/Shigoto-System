package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.Job;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "shigoto.demo-data.enabled=false")
class OptimisticLockingIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private long companyId;
    private long candidateId;
    private long interviewerId;
    private long jobId;
    private long applicationId;
    private long interviewId;

    @BeforeEach
    void createFixture() {
        long idBase = -(System.nanoTime() & Long.MAX_VALUE);
        companyId = idBase;
        candidateId = idBase - 1;
        interviewerId = idBase - 2;
        jobId = idBase - 3;
        applicationId = idBase - 4;
        interviewId = idBase - 5;

        inTransaction(entityManager -> {
            execute(entityManager, "insert into companies (id, name) values (?, ?)",
                    companyId, "Concurrency Company " + -idBase);
            execute(entityManager, """
                    insert into users (id, first_name, last_name, email, password, role, student)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, candidateId, "Concurrent", "Candidate",
                    "candidate-" + -idBase + "@example.com", "encoded-password", "CANDIDATE", false);
            execute(entityManager, """
                    insert into users (id, first_name, last_name, email, password, role, student, company_id)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """, interviewerId, "Concurrent", "Interviewer",
                    "interviewer-" + -idBase + "@example.com", "encoded-password", "INTERVIEWER", false,
                    companyId);
            execute(entityManager,
                    "insert into jobs (id, title, company_id, status) values (?, ?, ?, ?)",
                    jobId, "Concurrency Job", companyId, "OPEN");
            execute(entityManager, """
                    insert into applications (id, candidate_id, job_id, status, version)
                    values (?, ?, ?, ?, ?)
                    """, applicationId, candidateId, jobId, "APPLIED", 0L);
            execute(entityManager, """
                    insert into interviews
                        (id, application_id, interviewer_id, scheduled_at, meeting_link, type, status, version)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """, interviewId, applicationId, interviewerId, LocalDateTime.now().plusDays(7),
                    "https://meet.example.com/original", "TECHNICAL", "SCHEDULED", 0L);
        });
    }

    @AfterEach
    void removeFixture() {
        inTransaction(entityManager -> {
            execute(entityManager, "delete from interviews where id = ?", interviewId);
            execute(entityManager, "delete from applications where id = ?", applicationId);
            execute(entityManager, "delete from jobs where id = ?", jobId);
            execute(entityManager, "delete from users where id in (?, ?)", candidateId, interviewerId);
            execute(entityManager, "delete from companies where id = ?", companyId);
        });
    }

    @Test
    void staleApplicationUpdateFailsAndWinningValuesRemain() {
        EntityManager winnerManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        try {
            winnerManager.getTransaction().begin();
            staleManager.getTransaction().begin();
            Application winner = winnerManager.find(Application.class, applicationId);
            Application stale = staleManager.find(Application.class, applicationId);
            assertEquals(0L, winner.getVersion());
            assertEquals(0L, stale.getVersion());

            winner.setHrNotes("Winning HR update");
            winnerManager.getTransaction().commit();
            assertEquals(1L, winner.getVersion());

            stale.setHrNotes("Stale interviewer update");
            RollbackException exception = assertThrows(
                    RollbackException.class, staleManager.getTransaction()::commit);
            assertInstanceOf(OptimisticLockException.class, exception.getCause());
        } finally {
            rollbackAndClose(winnerManager);
            rollbackAndClose(staleManager);
        }

        Application persisted = find(Application.class, applicationId);
        assertEquals("Winning HR update", persisted.getHrNotes());
        assertEquals(1L, persisted.getVersion());
    }

    @Test
    void staleJobUpdateFailsAndWinningValuesRemain() {
        EntityManager winnerManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        try {
            winnerManager.getTransaction().begin();
            staleManager.getTransaction().begin();
            Job winner = winnerManager.find(Job.class, jobId);
            Job stale = staleManager.find(Job.class, jobId);
            assertEquals(0L, winner.getVersion());
            assertEquals(0L, stale.getVersion());

            winner.setTitle("Winning job title");
            winnerManager.getTransaction().commit();
            assertEquals(1L, winner.getVersion());

            stale.setDescription("Stale job description");
            RollbackException exception = assertThrows(
                    RollbackException.class, staleManager.getTransaction()::commit);
            assertInstanceOf(OptimisticLockException.class, exception.getCause());
        } finally {
            rollbackAndClose(winnerManager);
            rollbackAndClose(staleManager);
        }

        Job persisted = find(Job.class, jobId);
        assertEquals("Winning job title", persisted.getTitle());
        assertEquals(null, persisted.getDescription());
        assertEquals(1L, persisted.getVersion());
    }

    @Test
    void staleCancelFailsAfterConcurrentReschedule() {
        LocalDateTime winningTime = LocalDateTime.now().plusDays(10).withNano(0);
        EntityManager winnerManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        try {
            winnerManager.getTransaction().begin();
            staleManager.getTransaction().begin();
            Interview winner = winnerManager.find(Interview.class, interviewId);
            Interview stale = staleManager.find(Interview.class, interviewId);
            assertEquals(0L, winner.getVersion());
            assertEquals(0L, stale.getVersion());

            winner.setScheduledAt(winningTime);
            winner.setMeetingLink("https://meet.example.com/rescheduled");
            winnerManager.getTransaction().commit();
            assertEquals(1L, winner.getVersion());

            stale.setStatus(InterviewStatus.CANCELED);
            RollbackException exception = assertThrows(
                    RollbackException.class, staleManager.getTransaction()::commit);
            assertInstanceOf(OptimisticLockException.class, exception.getCause());
        } finally {
            rollbackAndClose(winnerManager);
            rollbackAndClose(staleManager);
        }

        Interview persisted = find(Interview.class, interviewId);
        assertEquals(InterviewStatus.SCHEDULED, persisted.getStatus());
        assertEquals(winningTime, persisted.getScheduledAt());
        assertEquals("https://meet.example.com/rescheduled", persisted.getMeetingLink());
        assertEquals(1L, persisted.getVersion());
    }

    @Test
    void staleSecondFeedbackFailsAndFirstFeedbackRemains() {
        EntityManager winnerManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        try {
            winnerManager.getTransaction().begin();
            staleManager.getTransaction().begin();
            Interview winner = winnerManager.find(Interview.class, interviewId);
            Interview stale = staleManager.find(Interview.class, interviewId);

            winner.setFeedback("First committed feedback");
            winner.setStatus(InterviewStatus.COMPLETED);
            winnerManager.getTransaction().commit();

            stale.setFeedback("Stale second feedback");
            stale.setStatus(InterviewStatus.COMPLETED);
            RollbackException exception = assertThrows(
                    RollbackException.class, staleManager.getTransaction()::commit);
            assertInstanceOf(OptimisticLockException.class, exception.getCause());
        } finally {
            rollbackAndClose(winnerManager);
            rollbackAndClose(staleManager);
        }

        Interview persisted = find(Interview.class, interviewId);
        assertEquals("First committed feedback", persisted.getFeedback());
        assertEquals(InterviewStatus.COMPLETED, persisted.getStatus());
        assertEquals(1L, persisted.getVersion());
    }

    private <T> T find(Class<T> entityType, long id) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return entityManager.find(entityType, id);
        } finally {
            entityManager.close();
        }
    }

    private void inTransaction(Consumer<EntityManager> work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            work.accept(entityManager);
            entityManager.getTransaction().commit();
        } catch (RuntimeException ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw ex;
        } finally {
            entityManager.close();
        }
    }

    private void execute(EntityManager entityManager, String sql, Object... parameters) {
        var query = entityManager.createNativeQuery(sql);
        for (int index = 0; index < parameters.length; index++) {
            query.setParameter(index + 1, parameters[index]);
        }
        query.executeUpdate();
    }

    private void rollbackAndClose(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }
}
