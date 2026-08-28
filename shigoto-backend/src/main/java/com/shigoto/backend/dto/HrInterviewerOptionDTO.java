package com.shigoto.backend.dto;

import com.shigoto.backend.entity.User;

public record HrInterviewerOptionDTO(Long interviewerId, String fullName, String email) {
    public static HrInterviewerOptionDTO from(User interviewer) {
        return new HrInterviewerOptionDTO(
                interviewer.getId(),
                (interviewer.getFirstName() + " " + interviewer.getLastName()).trim(),
                interviewer.getEmail());
    }
}
