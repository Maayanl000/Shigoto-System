package com.shigoto.backend.service;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthenticatedUserResponseDTO registerCandidate(RegisterRequestDTO request) {
        return authService.registerCandidate(request);
    }

    public List<AuthenticatedUserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AuthenticatedUserResponseDTO::from)
                .toList();
    }
}
