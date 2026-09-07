package com.shigoto.backend.dto;

/**
 * Defines replacement internal task-review notes and the expected application version.
 */
public record InterviewerTaskReviewNotesRequestDTO(String taskReviewNotes, Long version) {}
