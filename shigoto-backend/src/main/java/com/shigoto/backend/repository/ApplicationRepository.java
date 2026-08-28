package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Application;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // שאילתת עזר שנצטרך בעתיד: למצוא את כל המועמדויות של מועמד ספציפי לפי ה-ID שלו
    List<Application> findByCandidateIdOrderByAppliedAtDesc(Long candidateId);

    // שאילתת עזר: למצוא את כל המועמדויות ששייכות למשרה ספציפית
    List<Application> findByJobId(Long jobId);

    List<Application> findByJobCompany(Company company);

    Optional<Application> findByIdAndJobCompany(Long id, Company company);

    List<Application> findByStatusAndJobCompanyOrderByAppliedAtAsc(
            ApplicationStatus status, Company company);

    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);
}
