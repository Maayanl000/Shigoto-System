package com.shigoto.backend.dto;

import com.shigoto.backend.entity.User;

/**
 * Represents an interviewer available for selection within an HR user's company.
 */
public record HrInterviewerOptionDTO(Long interviewerId, String fullName, String email) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param interviewer the authenticated interviewer
     * @return a DTO populated from the supplied domain entity
     */
    public static HrInterviewerOptionDTO from(User interviewer) {
        return new HrInterviewerOptionDTO(
                interviewer.getId(),
                (interviewer.getFirstName() + " " + interviewer.getLastName()).trim(),
                interviewer.getEmail());
    }
}
