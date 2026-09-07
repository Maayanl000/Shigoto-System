package com.shigoto.backend.dto;

import com.shigoto.backend.entity.ApplicationStatus;
import com.shigoto.backend.entity.Interview;
import com.shigoto.backend.entity.InterviewStatus;
import com.shigoto.backend.entity.InterviewType;

import java.time.LocalDateTime;

/**
 * Represents full interview scheduling and assignment details visible to HR.
 */
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
        String feedback,
        Long version,
        Long applicationVersion
) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param interview the interview being processed
     * @return a DTO populated from the supplied domain entity
     */
    public static HrScheduledInterviewResponseDTO from(Interview interview) {
        var interviewer = interview.getInterviewer();
        return new HrScheduledInterviewResponseDTO(
                interview.getId(), interview.getApplication().getId(), interviewer.getId(),
                (interviewer.getFirstName() + " " + interviewer.getLastName()).trim(),
                interview.getScheduledAt(), interview.getMeetingLink(), interview.getType(),
                interview.getStatus(), interview.getApplication().getStatus(), interview.getFeedback(),
                interview.getVersion(), interview.getApplication().getVersion());
    }
}
