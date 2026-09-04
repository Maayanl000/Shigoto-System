package com.shigoto.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "shigoto.demo-data.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
class QaApplicationLoadIntegrationTest {

    private static final int[] SCALES = {10, 50, 100, 500, 1_000};

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void currentDatabaseStateIsReportedWithoutMutation() {
        List<String> duplicateHrCompanies = jdbcTemplate.queryForList("""
                SELECT company_id || ':' || COUNT(*)
                FROM users
                WHERE role = 'HR' AND company_id IS NOT NULL
                GROUP BY company_id
                HAVING COUNT(*) > 1
                ORDER BY company_id
                """, String.class);
        List<String> oneHrIndexes = jdbcTemplate.queryForList("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'uk_users_one_hr_per_company'
                """, String.class);
        List<String> companyHrUsers = jdbcTemplate.queryForList("""
                SELECT company_id || ':' || id || ':' || email
                FROM users
                WHERE role = 'HR' AND company_id IS NOT NULL
                ORDER BY company_id, id
                """, String.class);
        Integer leftoverQaUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email LIKE 'qa-load-%'", Integer.class);

        System.out.println("QA_DB_STATE duplicateHrCompanies=" + duplicateHrCompanies
                + " oneHrIndex=" + oneHrIndexes + " companyHrUsers=" + companyHrUsers
                + " leftoverQaUsers=" + leftoverQaUsers);
        assertEquals(0, leftoverQaUsers);
    }

    @Test
    void controlledUniqueCandidateLoadAndDuplicateRacePreserveIntegrity() throws Exception {
        String token = UUID.randomUUID().toString().replace("-", "");
        String emailPrefix = "qa-load-" + token + "-";
        long companyId = insertCompany("QA Load " + token);
        long jobId = insertJob(companyId, "QA Load Job " + token);
        long duplicateRaceJobId = insertJob(companyId, "QA Duplicate Race " + token);
        List<Long> candidateIds = insertCandidates(emailPrefix, 1_001);

        try {
            int submitted = 0;
            for (int scale : SCALES) {
                List<Long> batch = candidateIds.subList(submitted, scale);
                Instant started = Instant.now();
                Outcomes outcomes = insertApplicationsConcurrently(batch, jobId);
                long elapsedMillis = Duration.between(started, Instant.now()).toMillis();
                int finalCount = countApplications(jobId);

                System.out.printf(
                        "QA_LOAD_RESULT scale=%d success=%d expectedRejections=%d unexpected=%d elapsedMs=%d throughputPerSec=%.2f finalCount=%d%n",
                        scale, outcomes.success(), outcomes.conflict(), outcomes.unexpected(), elapsedMillis,
                        elapsedMillis == 0 ? outcomes.success() : outcomes.success() * 1000.0 / elapsedMillis,
                        finalCount);

                assertEquals(batch.size(), outcomes.success());
                assertEquals(0, outcomes.conflict());
                assertEquals(0, outcomes.unexpected());
                assertEquals(scale, finalCount);
                submitted = scale;
            }

            Outcomes duplicateRace = insertApplicationsConcurrently(
                    java.util.Collections.nCopies(20, candidateIds.get(1_000)), duplicateRaceJobId);
            int duplicateFinalCount = countApplications(duplicateRaceJobId);
            System.out.printf(
                    "QA_DUPLICATE_RACE attempts=20 success=%d expectedRejections=%d unexpected=%d finalCount=%d%n",
                    duplicateRace.success(), duplicateRace.conflict(), duplicateRace.unexpected(), duplicateFinalCount);

            assertEquals(1, duplicateRace.success());
            assertEquals(19, duplicateRace.conflict());
            assertEquals(0, duplicateRace.unexpected());
            assertEquals(1, duplicateFinalCount);

            Integer duplicateGroups = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM (
                        SELECT candidate_id, job_id
                        FROM applications
                        WHERE job_id IN (?, ?)
                        GROUP BY candidate_id, job_id
                        HAVING COUNT(*) > 1
                    ) duplicates
                    """, Integer.class, jobId, duplicateRaceJobId);
            assertEquals(0, duplicateGroups);
        } finally {
            jdbcTemplate.update("DELETE FROM applications WHERE job_id IN (?, ?)", jobId, duplicateRaceJobId);
            jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", emailPrefix + "%");
            jdbcTemplate.update("DELETE FROM jobs WHERE id IN (?, ?)", jobId, duplicateRaceJobId);
            jdbcTemplate.update("DELETE FROM companies WHERE id = ?", companyId);
        }
    }

    private Outcomes insertApplicationsConcurrently(List<Long> candidateIds, long jobId) throws Exception {
        int poolSize = Math.min(32, candidateIds.size());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (Long candidateId : candidateIds) {
                Callable<Outcome> task = () -> {
                    start.await();
                    try {
                        jdbcTemplate.update("""
                                INSERT INTO applications
                                    (candidate_id, job_id, status, cv_url, cover_letter,
                                     applied_at, status_changed_at, version)
                                VALUES (?, ?, 'APPLIED', ?, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                                """, candidateId, jobId, UUID.randomUUID() + ".pdf");
                        return Outcome.SUCCESS;
                    } catch (DataIntegrityViolationException expectedDuplicate) {
                        return Outcome.CONFLICT;
                    } catch (RuntimeException unexpected) {
                        System.out.println("QA_UNEXPECTED " + unexpected.getClass().getName()
                                + ": " + unexpected.getMessage());
                        return Outcome.UNEXPECTED;
                    }
                };
                futures.add(executor.submit(task));
            }
            start.countDown();

            int success = 0;
            int conflict = 0;
            int unexpected = 0;
            for (Future<Outcome> future : futures) {
                switch (future.get()) {
                    case SUCCESS -> success++;
                    case CONFLICT -> conflict++;
                    case UNEXPECTED -> unexpected++;
                }
            }
            return new Outcomes(success, conflict, unexpected);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private long insertCompany(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO companies (name) VALUES (?) RETURNING id", Long.class, name);
    }

    private long insertJob(long companyId, String title) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO jobs (company_id, title, description, location, status, created_at, version)
                VALUES (?, ?, 'QA load test', 'QA', 'OPEN', CURRENT_TIMESTAMP, 0)
                RETURNING id
                """, Long.class, companyId, title);
    }

    private List<Long> insertCandidates(String emailPrefix, int count) {
        List<Object[]> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{"QA", "Candidate " + i, emailPrefix + i + "@example.test", "unused", "CANDIDATE"});
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO users (first_name, last_name, email, password, role)
                VALUES (?, ?, ?, ?, ?)
                """, rows);
        return jdbcTemplate.queryForList(
                "SELECT id FROM users WHERE email LIKE ? ORDER BY email", Long.class, emailPrefix + "%");
    }

    private int countApplications(long jobId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applications WHERE job_id = ?", Integer.class, jobId);
    }

    private enum Outcome { SUCCESS, CONFLICT, UNEXPECTED }

    private record Outcomes(int success, int conflict, int unexpected) {}
}
