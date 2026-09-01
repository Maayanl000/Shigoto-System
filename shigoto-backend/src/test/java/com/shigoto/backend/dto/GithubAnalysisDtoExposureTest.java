package com.shigoto.backend.dto;

import com.shigoto.backend.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubAnalysisDtoExposureTest {
    @Test
    void hrAndInterviewerApplicationDtosExposeSafeAnalysis() {
        User candidate = User.builder().id(1L).firstName("Dana").lastName("Cohen")
                .email("dana@example.com").role(Role.CANDIDATE)
                .githubProfileUrl("https://github.com/dana").build();
        GithubData data = GithubData.builder().id(2L).candidate(candidate).username("dana")
                .status(GithubAnalysisStatus.READY).publicRepositoryCount(4)
                .topLanguages(List.of("Java", "TypeScript", "Python", "Go", "Shell"))
                .latestPushAt(LocalDateTime.of(2026, 8, 1, 12, 0))
                .analyzedAt(LocalDateTime.of(2026, 8, 2, 12, 0)).lastEventId(UUID.randomUUID()).build();
        candidate.setGithubData(data);
        Company company = Company.builder().id(3L).name("Shigoto").build();
        Job job = Job.builder().id(4L).title("Backend Engineer").location("Remote").company(company).build();
        Application application = Application.builder().id(5L).candidate(candidate).job(job)
                .status(ApplicationStatus.APPLIED).build();

        GithubAnalysisDTO hrAnalysis = HrApplicationDetailsDTO.from(application).githubAnalysis();
        GithubAnalysisDTO interviewerAnalysis = InterviewerCandidateReviewDTO.from(application).githubAnalysis();

        assertEquals(GithubAnalysisStatus.READY, hrAnalysis.status());
        assertEquals(List.of("Java", "TypeScript", "Python", "Go", "Shell"),
                hrAnalysis.topLanguages());
        assertEquals(hrAnalysis, interviewerAnalysis);
    }
}
