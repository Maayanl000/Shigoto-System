package com.shigoto.backend.dto;

import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

public record HrScheduledInterviewResponseDTO(
        Long interviewId,
        Long applicationId,
        Long interviewerId,
        String interviewerName,
        LocalDateTime scheduledAt,
        String meetingLink,
        InterviewType type,
        InterviewStatus status,
        ApplicationStatus applicationStatus,
        String feedback
) {
    public static HrScheduledInterviewResponseDTO from(Interview interview) {
        var interviewer = interview.getInterviewer();
        return new HrScheduledInterviewResponseDTO(
                interview.getId(), interview.getApplication().getId(), interviewer.getId(),
                (interviewer.getFirstName() + " " + interviewer.getLastName()).trim(),
                interview.getScheduledAt(), interview.getMeetingLink(), interview.getType(),
                interview.getStatus(), interview.getApplication().getStatus(), interview.getFeedback());
    }
}
