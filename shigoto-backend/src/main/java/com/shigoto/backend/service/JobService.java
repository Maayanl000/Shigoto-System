package com.shigoto.backend.service;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.entity.JobStatus;
import com.shigoto.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    // פונקציה ליצירת משרה חדשה
    public Job createJob(Job job) {
        // כאן בעתיד נוכל להוסיף לוגיקה עסקית, למשל:
        // קביעת סטטוס ברירת מחדל של "פתוחה" למשרה חדשה שנוצרת
        return jobRepository.save(job);
    }

    // פונקציה לשליפת כל המשרות (ישמש אותנו לדף הבית של האורחים/מועמדים)
    public List<Job> getOpenJobs() {
        return jobRepository.findByStatus(JobStatus.OPEN);
    }
}
