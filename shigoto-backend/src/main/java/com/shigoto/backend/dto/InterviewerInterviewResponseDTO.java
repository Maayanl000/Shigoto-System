package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

/**
 * Represents an assigned interview and its review state for an interviewer.
 */
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
        String interviewerNotes,
        Long version
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param interview the interview being processed
     * @return a DTO populated from the supplied domain entity
     */
    public static InterviewerInterviewResponseDTO from(Interview interview) {
        var application = interview.getApplication();
        var candidate = application.getCandidate();
        var job = application.getJob();
        return new InterviewerInterviewResponseDTO(
                interview.getId(), application.getId(),
                (candidate.getFirstName() + " " + candidate.getLastName()).trim(),
                job.getTitle(), job.getCompany().getName(), interview.getType(),
                interview.getScheduledAt(), interview.getMeetingLink(), interview.getStatus(),
                interview.getFeedback(), interview.getInterviewerNotes(), interview.getVersion());
    }
}
