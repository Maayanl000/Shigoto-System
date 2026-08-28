package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

public record InterviewerInterviewResponseDTO(
        Long interviewId,
        Long applicationId,
        String candidateName,
        String jobTitle,
        String companyName,
        InterviewType interviewType,
        LocalDateTime scheduledAt,
        String meetingLink,
        InterviewStatus status,
        String feedback,
        String interviewerNotes
) {
    public static InterviewerInterviewResponseDTO from(Interview interview) {
        var application = interview.getApplication();
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new InterviewerInterviewResponseDTO(
                interview.getId(), application.getId(),
                (candidate.getFirstName() + " " + candidate.getLastName()).trim(),
                job.getTitle(), job.getCompany().getName(), interview.getType(),
                interview.getScheduledAt(), interview.getMeetingLink(), interview.getStatus(),
                interview.getFeedback(), interview.getInterviewerNotes());
    }
}
