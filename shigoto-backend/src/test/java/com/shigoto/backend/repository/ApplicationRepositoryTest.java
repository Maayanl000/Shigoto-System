package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.Company;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findsOnlyApplicationsForJobsBelongingToSpecifiedCompany() {
        long idBase = -(System.nanoTime() & Long.MAX_VALUE);
        long companyAId = idBase;
        long companyBId = idBase - 1;
        long jobAId = idBase - 2;
        long jobBId = idBase - 3;
        long candidateId = idBase - 4;
        long applicationAId = idBase - 5;
        long applicationBId = idBase - 6;

        insert("insert into companies (id, name) values (?, ?)", companyAId, "Company A " + idBase);
        insert("insert into companies (id, name) values (?, ?)", companyBId, "Company B " + idBase);
        insert("insert into jobs (id, title, company_id, status) values (?, ?, ?, ?)",
                jobAId, "Job A", companyAId, "OPEN");
        insert("insert into jobs (id, title, company_id, status) values (?, ?, ?, ?)",
                jobBId, "Job B", companyBId, "OPEN");
        insert("""
                insert into users (id, first_name, last_name, email, password, role, student)
                values (?, ?, ?, ?, ?, ?, ?)
                """, candidateId, "Test", "Candidate", "candidate-" + idBase + "@example.com",
                "encoded-password", "CANDIDATE", false);
        insert("insert into applications (id, candidate_id, job_id, status) values (?, ?, ?, ?)",
                applicationAId, candidateId, jobAId, "APPLIED");
        insert("insert into applications (id, candidate_id, job_id, status) values (?, ?, ?, ?)",
                applicationBId, candidateId, jobBId, "APPLIED");
        entityManager.clear();

        Company companyA = entityManager.find(Company.class, companyAId);
        Application applicationA = entityManager.find(Application.class, applicationAId);
        Application applicationB = entityManager.find(Application.class, applicationBId);

        var results = applicationRepository.findByJobCompany(companyA);

        assertTrue(results.stream().anyMatch(application -> application.getId().equals(applicationA.getId())));
        assertFalse(results.stream().anyMatch(application -> application.getId().equals(applicationB.getId())));
    }

    private void insert(String sql, Object... parameters) {
        var query = entityManager.createNativeQuery(sql);
        for (int index = 0; index < parameters.length; index++) {
            query.setParameter(index + 1, parameters[index]);
        }
        query.executeUpdate();
    }
}
