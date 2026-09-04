package com.shigoto.backend.service;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthService authService;

    public AuthenticatedUserResponseDTO registerCandidate(RegisterRequestDTO request) {
        return authService.registerCandidate(request);
    }
}
