package com.shigoto.backend.controller;

import com.shigoto.backend.entity.Job;
import com.shigoto.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        // קריאה ללוגיקה העסקית ליצירת המשרה ושמירתה
        Job savedJob = jobService.createJob(job);

        // החזרת תשובת HTTP 200 עם אובייקט המשרה שנוצר
        return ResponseEntity.ok(savedJob);
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        // שליפת כל המשרות הקיימות במערכת
        return ResponseEntity.ok(jobService.getAllJobs());
    }
}