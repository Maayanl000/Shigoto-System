package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // עוד קסם של Spring - פונקציה שתחזיר לנו רשימה של משרות לפי הסטטוס שלהן!
    List<Job> findByStatus(JobStatus status);
}