package com.shigoto.backend.dto;

/**
 * Defines replacement internal HR notes and the expected application version.
 */
public record HrNotesUpdateRequestDTO(String hrNotes, Long version) {
}
