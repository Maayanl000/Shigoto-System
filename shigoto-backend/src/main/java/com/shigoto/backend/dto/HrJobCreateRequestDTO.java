package com.shigoto.backend.dto;

/**
 * Defines the title, description, and location submitted when HR creates a job.
 */
public record HrJobCreateRequestDTO(String title, String description, String location) {}
