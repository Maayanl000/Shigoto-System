package com.shigoto.backend.service;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.LoginRequestDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.DuplicateEmailException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticatedUserResponseDTO registerCandidate(RegisterRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration details are required");
        }

        String firstName = requireName(request.firstName(), "First name");
        String lastName = requireName(request.lastName(), "Last name");
        String email = normalizeAndValidateEmail(request.email());
        validatePassword(request.password());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("User with this email already exists");
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CANDIDATE)
                .build();

        try {
            return AuthenticatedUserResponseDTO.from(userRepository.save(user));
        } catch (DataIntegrityViolationException ex) {
            // Preserve a clear 409 response if two registrations race past the pre-check.
            throw new DuplicateEmailException("User with this email already exists");
        }
    }

    public Authentication authenticate(LoginRequestDTO request) {
        if (request == null || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String email = normalizeAndValidateEmail(request.email());
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
        );
    }

    public AuthenticatedUserResponseDTO getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Authenticated user was not found");
        }
        return AuthenticatedUserResponseDTO.from(findByEmail(authentication.getName()));
    }

    public User getAuthenticatedCandidate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Authenticated user was not found");
        }
        User user = findByEmail(authentication.getName());
        if (user.getRole() != Role.CANDIDATE) {
            throw new AccessDeniedException("Candidate access is required");
        }
        return user;
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String requireName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeAndValidateEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email is invalid");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MINIMUM_PASSWORD_LENGTH + " characters"
            );
        }
    }
}
