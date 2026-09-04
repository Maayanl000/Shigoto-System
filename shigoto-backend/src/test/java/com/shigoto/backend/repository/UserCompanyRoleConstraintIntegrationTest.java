package com.shigoto.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "shigoto.demo-data.enabled=false",
        "spring.sql.init.mode=never"
})
@Transactional
class UserCompanyRoleConstraintIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createIsolatedUsersTableWithProductionConstraint() {
        jdbcTemplate.execute("""
                CREATE TEMPORARY TABLE one_hr_constraint_users (
                    id BIGSERIAL PRIMARY KEY,
                    company_id BIGINT,
                    role VARCHAR(32) NOT NULL
                ) ON COMMIT DROP
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX uk_test_users_one_hr_per_company
                    ON one_hr_constraint_users (company_id)
                    WHERE role = 'HR' AND company_id IS NOT NULL
                """);
    }

    @Test
    void firstHrPerCompanyAndOneHrForAnotherCompanyAreAllowed() {
        insertUser(1L, "HR");
        insertUser(2L, "HR");

        assertEquals(2, countUsers("HR"));
    }

    @Test
    void secondHrForSameCompanyIsRejected() {
        insertUser(1L, "HR");

        assertThrows(DataIntegrityViolationException.class, () -> insertUser(1L, "HR"));
    }

    @Test
    void multipleInterviewersForSameCompanyRemainAllowed() {
        insertUser(1L, "INTERVIEWER");
        insertUser(1L, "INTERVIEWER");

        assertEquals(2, countUsers("INTERVIEWER"));
    }

    @Test
    void productionSchemaContainsTheVerifiedPartialUniqueIndex() throws IOException {
        String schema = new ClassPathResource("schema-postgresql.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(schema.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_users_one_hr_per_company"));
        assertTrue(schema.contains("ON public.users (company_id)"));
        assertTrue(schema.contains("WHERE role = 'HR' AND company_id IS NOT NULL"));
    }

    private void insertUser(long companyId, String role) {
        jdbcTemplate.update(
                "INSERT INTO one_hr_constraint_users (company_id, role) VALUES (?, ?)",
                companyId, role);
    }

    private int countUsers(String role) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM one_hr_constraint_users WHERE role = ?",
                Integer.class, role);
    }
}
