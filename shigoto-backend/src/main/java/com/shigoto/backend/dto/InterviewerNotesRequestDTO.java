package com.shigoto.backend.dto;

/**
 * Defines replacement internal interview notes and the expected interview version.
 */
public record InterviewerNotesRequestDTO(String interviewerNotes, Long version) {}
