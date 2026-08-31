package com.shigoto.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "shigoto.demo-data.enabled=false")
class InterviewSlotConcurrencyIntegrationTest {

    private static final String ACTIVE_SLOT_INDEX = "uk_interviews_active_interviewer_slot";

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private long companyId;
    private long candidateOneId;
    private long candidateTwoId;
    private long interviewerId;
    private long jobId;
    private long applicationOneId;
    private long applicationTwoId;
    private long interviewOneId;
    private long interviewTwoId;

    @BeforeEach
    void createFixture() {
        long idBase = -(System.nanoTime() & Long.MAX_VALUE);
        companyId = idBase;
        candidateOneId = idBase - 1;
        candidateTwoId = idBase - 2;
        interviewerId = idBase - 3;
        jobId = idBase - 4;
        applicationOneId = idBase - 5;
        applicationTwoId = idBase - 6;
        interviewOneId = idBase - 7;
        interviewTwoId = idBase - 8;

        inTransaction(entityManager -> {
            execute(entityManager, "insert into companies (id, name) values (?, ?)",
                    companyId, "Slot Concurrency Company " + -idBase);
            execute(entityManager, """
                    insert into users (id, first_name, last_name, email, password, role, student)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, candidateOneId, "First", "Candidate",
                    "slot-candidate-one-" + -idBase + "@example.com", "encoded", "CANDIDATE", false);
            execute(entityManager, """
                    insert into users (id, first_name, last_name, email, password, role, student)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, candidateTwoId, "Second", "Candidate",
                    "slot-candidate-two-" + -idBase + "@example.com", "encoded", "CANDIDATE", false);
            execute(entityManager, """
                    insert into users (id, first_name, last_name, email, password, role, student, company_id)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """, interviewerId, "Slot", "Interviewer",
                    "slot-interviewer-" + -idBase + "@example.com", "encoded", "INTERVIEWER", false, companyId);
            execute(entityManager,
                    "insert into jobs (id, title, company_id, status) values (?, ?, ?, ?)",
                    jobId, "Concurrency Job", companyId, "OPEN");
            execute(entityManager, """
                    insert into applications (id, candidate_id, job_id, status, version)
                    values (?, ?, ?, ?, ?)
                    """, applicationOneId, candidateOneId, jobId, "TECH_INTERVIEW_SCHEDULED", 0L);
            execute(entityManager, """
                    insert into applications (id, candidate_id, job_id, status, version)
                    values (?, ?, ?, ?, ?)
                    """, applicationTwoId, candidateTwoId, jobId, "TECH_INTERVIEW_SCHEDULED", 0L);
        });
    }

    @AfterEach
    void removeFixture() {
        inTransaction(entityManager -> {
            execute(entityManager, "delete from interviews where id in (?, ?)", interviewOneId, interviewTwoId);
            execute(entityManager, "delete from applications where id in (?, ?)",
                    applicationOneId, applicationTwoId);
            execute(entityManager, "delete from jobs where id = ?", jobId);
            execute(entityManager, "delete from users where id in (?, ?, ?)",
                    candidateOneId, candidateTwoId, interviewerId);
            execute(entityManager, "delete from companies where id = ?", companyId);
        });
    }

    @Test
    void concurrentSchedulingAllowsExactlyOneActiveInterview() throws Exception {
        LocalDateTime slot = LocalDateTime.now().plusDays(20).withNano(0);
        CyclicBarrier bothObservedAvailable = new CyclicBarrier(2);

        List<AttemptResult> results = runConcurrently(
                () -> attemptInsert(interviewOneId, applicationOneId, slot, bothObservedAvailable),
                () -> attemptInsert(interviewTwoId, applicationTwoId, slot, bothObservedAvailable));

        assertTrue(results.stream().allMatch(AttemptResult::observedAvailable));
        assertEquals(1, results.stream().filter(AttemptResult::succeeded).count());
        assertEquals(1, results.stream().filter(AttemptResult::slotConflict).count());
        assertEquals(1L, countActiveAt(slot));
    }

    @Test
    void canceledInterviewDoesNotBlockSlotReuse() {
        LocalDateTime slot = LocalDateTime.now().plusDays(21).withNano(0);
        inTransaction(entityManager -> execute(entityManager, """
                insert into interviews
                    (id, application_id, interviewer_id, scheduled_at, meeting_link, type, status, version)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, interviewOneId, applicationOneId, interviewerId, slot,
                "https://meet.example.com/canceled", "MANAGER", "CANCELED", 0L));

        inTransaction(entityManager -> execute(entityManager, """
                insert into interviews
                    (id, application_id, interviewer_id, scheduled_at, meeting_link, type, status, version)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, interviewTwoId, applicationTwoId, interviewerId, slot,
                "https://meet.example.com/reused", "MANAGER", "SCHEDULED", 0L));

        assertEquals(1L, countActiveAt(slot));
        assertEquals(2L, countAllAt(slot));
    }

    @Test
    void concurrentReschedulingAllowsOnlyOneInterviewIntoTargetSlot() throws Exception {
        LocalDateTime firstOriginal = LocalDateTime.now().plusDays(22).withNano(0);
        LocalDateTime secondOriginal = firstOriginal.plusHours(1);
        LocalDateTime target = firstOriginal.plusHours(2);
        inTransaction(entityManager -> {
            insertScheduled(entityManager, interviewOneId, applicationOneId, firstOriginal);
            insertScheduled(entityManager, interviewTwoId, applicationTwoId, secondOriginal);
        });
        CyclicBarrier bothObservedAvailable = new CyclicBarrier(2);

        List<AttemptResult> results = runConcurrently(
                () -> attemptReschedule(interviewOneId, target, bothObservedAvailable),
                () -> attemptReschedule(interviewTwoId, target, bothObservedAvailable));

        assertTrue(results.stream().allMatch(AttemptResult::observedAvailable));
        assertEquals(1, results.stream().filter(AttemptResult::succeeded).count());
        assertEquals(1, results.stream().filter(AttemptResult::slotConflict).count());
        assertEquals(1L, countActiveAt(target));
        assertEquals(2L, countAllActiveForInterviewer());
    }

    @Test
    void expectedPostgresPartialUniqueIndexIsInstalled() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            String definition = (String) entityManager.createNativeQuery("""
                    select indexdef from pg_indexes
                    where schemaname = 'public' and indexname = ?
                    """, String.class).setParameter(1, ACTIVE_SLOT_INDEX).getSingleResult();
            assertTrue(definition.contains("UNIQUE INDEX"));
            assertTrue(definition.contains("(interviewer_id, scheduled_at)"));
            assertTrue(definition.contains("status"));
            assertTrue(definition.contains("'CANCELED'"));
        } finally {
            entityManager.close();
        }
    }

    private AttemptResult attemptInsert(
            long interviewId, long applicationId, LocalDateTime slot, CyclicBarrier barrier) {
        return attempt(slot, barrier, entityManager -> execute(entityManager, """
                insert into interviews
                    (id, application_id, interviewer_id, scheduled_at, meeting_link, type, status, version)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, interviewId, applicationId, interviewerId, slot,
                "https://meet.example.com/concurrent", "MANAGER", "SCHEDULED", 0L));
    }

    private AttemptResult attemptReschedule(long interviewId, LocalDateTime slot, CyclicBarrier barrier) {
        return attempt(slot, barrier, entityManager -> execute(entityManager, """
                update interviews set scheduled_at = ?, version = version + 1 where id = ?
                """, slot, interviewId));
    }

    private AttemptResult attempt(
            LocalDateTime slot, CyclicBarrier barrier, Consumer<EntityManager> write) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        boolean observedAvailable = false;
        try {
            entityManager.getTransaction().begin();
            observedAvailable = countActiveAt(entityManager, slot) == 0L;
            await(barrier);
            write.accept(entityManager);
            entityManager.getTransaction().commit();
            return new AttemptResult(observedAvailable, true, false);
        } catch (RuntimeException failure) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            return new AttemptResult(observedAvailable, false, isSlotConflict(failure));
        } finally {
            entityManager.close();
        }
    }

    private List<AttemptResult> runConcurrently(
            Callable<AttemptResult> first, Callable<AttemptResult> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<AttemptResult> firstResult = executor.submit(first);
            Future<AttemptResult> secondResult = executor.submit(second);
            return List.of(firstResult.get(20, TimeUnit.SECONDS), secondResult.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private void insertScheduled(
            EntityManager entityManager, long interviewId, long applicationId, LocalDateTime slot) {
        execute(entityManager, """
                insert into interviews
                    (id, application_id, interviewer_id, scheduled_at, meeting_link, type, status, version)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, interviewId, applicationId, interviewerId, slot,
                "https://meet.example.com/original", "MANAGER", "SCHEDULED", 0L);
    }

    private long countActiveAt(LocalDateTime slot) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return countActiveAt(entityManager, slot);
        } finally {
            entityManager.close();
        }
    }

    private long countActiveAt(EntityManager entityManager, LocalDateTime slot) {
        return ((Number) entityManager.createNativeQuery("""
                select count(*) from interviews
                where interviewer_id = ? and scheduled_at = ? and status <> 'CANCELED'
                """).setParameter(1, interviewerId).setParameter(2, slot).getSingleResult()).longValue();
    }

    private long countAllAt(LocalDateTime slot) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return ((Number) entityManager.createNativeQuery("""
                    select count(*) from interviews where interviewer_id = ? and scheduled_at = ?
                    """).setParameter(1, interviewerId).setParameter(2, slot).getSingleResult()).longValue();
        } finally {
            entityManager.close();
        }
    }

    private long countAllActiveForInterviewer() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return ((Number) entityManager.createNativeQuery("""
                    select count(*) from interviews where interviewer_id = ? and status <> 'CANCELED'
                    """).setParameter(1, interviewerId).getSingleResult()).longValue();
        } finally {
            entityManager.close();
        }
    }

    private boolean isSlotConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintFailure
                    && ACTIVE_SLOT_INDEX.equals(constraintFailure.getConstraintName())) return true;
            if (cause instanceof PSQLException postgresFailure
                    && postgresFailure.getServerErrorMessage() != null
                    && ACTIVE_SLOT_INDEX.equals(postgresFailure.getServerErrorMessage().getConstraint())) return true;
        }
        return false;
    }

    private void await(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new IllegalStateException("Concurrent attempts did not reach the write barrier", failure);
        }
    }

    private void inTransaction(Consumer<EntityManager> work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            work.accept(entityManager);
            entityManager.getTransaction().commit();
        } catch (RuntimeException failure) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw failure;
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

    private record AttemptResult(boolean observedAvailable, boolean succeeded, boolean slotConflict) {}
}
