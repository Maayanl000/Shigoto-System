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
        long secondJobAId = idBase - 4;
        long candidateId = idBase - 5;
        long applicationAId = idBase - 6;
        long applicationBId = idBase - 7;
        long secondApplicationAId = idBase - 8;

        insert("insert into companies (id, name) values (?, ?)", companyAId, "Company A " + idBase);
        insert("insert into companies (id, name) values (?, ?)", companyBId, "Company B " + idBase);
        insert("insert into jobs (id, title, company_id, status) values (?, ?, ?, ?)",
                jobAId, "Job A", companyAId, "OPEN");
        insert("insert into jobs (id, title, company_id, status) values (?, ?, ?, ?)",
                jobBId, "Job B", companyBId, "OPEN");
        insert("insert into jobs (id, title, company_id, status) values (?, ?, ?, ?)",
                secondJobAId, "Second Job A", companyAId, "CLOSED");
        insert("""
                insert into users (id, first_name, last_name, email, password, role, student)
                values (?, ?, ?, ?, ?, ?, ?)
                """, candidateId, "Test", "Candidate", "candidate-" + idBase + "@example.com",
                "encoded-password", "CANDIDATE", false);
        insert("insert into applications (id, candidate_id, job_id, status, version, hr_notes) "
                        + "values (?, ?, ?, ?, ?, ?)",
                applicationAId, candidateId, jobAId, "APPLIED", 0L, "COMPANY_A_PRIVATE");
        insert("insert into applications (id, candidate_id, job_id, status, version, hr_notes) "
                        + "values (?, ?, ?, ?, ?, ?)",
                applicationBId, candidateId, jobBId, "APPLIED", 0L, "COMPANY_B_PRIVATE");
        insert("insert into applications (id, candidate_id, job_id, status, version, hr_notes) "
                        + "values (?, ?, ?, ?, ?, ?)",
                secondApplicationAId, candidateId, secondJobAId, "APPLIED", 0L, "COMPANY_A_SECOND_PRIVATE");
        entityManager.clear();

        Company companyA = entityManager.find(Company.class, companyAId);
        Application applicationA = entityManager.find(Application.class, applicationAId);
        Application applicationB = entityManager.find(Application.class, applicationBId);
        Application secondApplicationA = entityManager.find(Application.class, secondApplicationAId);

        var results = applicationRepository.findByJobCompany(companyA);
        var jobResults = applicationRepository.findByJobIdAndJobCompany(jobAId, companyA);
        var foreignJobResults = applicationRepository.findByJobIdAndJobCompany(jobBId, companyA);
        var candidateCompanyResults =
                applicationRepository.findByCandidateIdAndJobCompanyOrderByAppliedAtDesc(candidateId, companyA);
        var ownApplicationById = applicationRepository.findByIdAndJobCompany(applicationAId, companyA);
        var foreignApplicationById = applicationRepository.findByIdAndJobCompany(applicationBId, companyA);

        assertTrue(results.stream().anyMatch(application -> application.getId().equals(applicationA.getId())));
        assertTrue(results.stream().anyMatch(application -> application.getId().equals(secondApplicationA.getId())));
        assertFalse(results.stream().anyMatch(application -> application.getId().equals(applicationB.getId())));
        assertTrue(jobResults.stream().anyMatch(application -> application.getId().equals(applicationA.getId())));
        assertFalse(jobResults.stream().anyMatch(application -> application.getId().equals(secondApplicationA.getId())));
        assertFalse(jobResults.stream().anyMatch(application -> application.getId().equals(applicationB.getId())));
        assertTrue(foreignJobResults.isEmpty());
        assertTrue(candidateCompanyResults.stream()
                .anyMatch(application -> application.getId().equals(applicationA.getId())));
        assertTrue(candidateCompanyResults.stream()
                .anyMatch(application -> application.getId().equals(secondApplicationA.getId())));
        assertFalse(candidateCompanyResults.stream()
                .anyMatch(application -> application.getId().equals(applicationB.getId())));
        assertFalse(candidateCompanyResults.stream()
                .anyMatch(application -> "COMPANY_B_PRIVATE".equals(application.getHrNotes())));
        assertTrue(ownApplicationById.isPresent());
        assertTrue(foreignApplicationById.isEmpty());
    }

    private void insert(String sql, Object... parameters) {
        var query = entityManager.createNativeQuery(sql);
        for (int index = 0; index < parameters.length; index++) {
            query.setParameter(index + 1, parameters[index]);
        }
        query.executeUpdate();
    }
}
