package com.shigoto.backend.service;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Provides the legacy candidate-registration entry point through the authentication service.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthService authService;

    /**
     * Registers a candidate after validating identity fields and enforcing email uniqueness.
     * @param request the request payload
     * @return a DTO representing the newly persisted candidate
     */
    public AuthenticatedUserResponseDTO registerCandidate(RegisterRequestDTO request) {
        return authService.registerCandidate(request);
    }
}
