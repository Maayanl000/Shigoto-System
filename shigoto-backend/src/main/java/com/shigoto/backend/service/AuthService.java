package com.shigoto.backend.service;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.CandidateProfileUpdateRequestDTO;
import com.shigoto.backend.dto.LoginRequestDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.DuplicateEmailException;
import com.shigoto.backend.exception.ResourceNotFoundException;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.repository.GithubDataRepository;
import com.shigoto.backend.util.GithubProfileUrlParser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GithubDataRepository githubDataRepository;

    public AuthenticatedUserResponseDTO registerCandidate(RegisterRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration details are required");
        }

        String firstName = requireName(request.firstName(), "First name");
        String lastName = requireName(request.lastName(), "Last name");
        String email = normalizeAndValidateEmail(request.email());
        validatePassword(request.password());
        if (request.githubProfileUrl() == null || request.githubProfileUrl().isBlank()) {
            throw new IllegalArgumentException("GitHub profile URL is required");
        }
        String githubProfileUrl = normalizeGithubProfileUrl(request.githubProfileUrl());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("User with this email already exists");
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CANDIDATE)
                .githubProfileUrl(githubProfileUrl)
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

    public User getAuthenticatedHr(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Authenticated user was not found");
        }
        User user = findByEmail(authentication.getName());
        if (user.getRole() != Role.HR) {
            throw new AccessDeniedException("HR access is required");
        }
        if (user.getCompany() == null) {
            throw new AccessDeniedException("HR user must belong to a company");
        }
        return user;
    }

    public User getAuthenticatedInterviewer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Authenticated user was not found");
        }
        User user = findByEmail(authentication.getName());
        if (user.getRole() != Role.INTERVIEWER) {
            throw new AccessDeniedException("Interviewer access is required");
        }
        if (user.getCompany() == null) {
            throw new AccessDeniedException("Interviewer must belong to a company");
        }
        return user;
    }

    @Transactional
    public AuthenticatedUserResponseDTO updateCandidateProfile(
            CandidateProfileUpdateRequestDTO request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Profile details are required");
        }
        User candidate = getAuthenticatedCandidate(authentication);
        String githubProfileUrl = normalizeGithubProfileUrl(request.githubProfileUrl());
        String previousUsername = GithubProfileUrlParser.extractUsername(candidate.getGithubProfileUrl()).orElse(null);
        String updatedUsername = GithubProfileUrlParser.extractUsername(githubProfileUrl).orElse(null);
        candidate.setFirstName(requireName(request.firstName(), "First name"));
        candidate.setLastName(requireName(request.lastName(), "Last name"));
        candidate.setGithubProfileUrl(githubProfileUrl);
        candidate.setCurrentTitle(normalizeOptionalProfileText(request.currentTitle(), "Current title"));
        candidate.setDesiredRole(normalizeOptionalProfileText(request.desiredRole(), "Desired role"));
        candidate.setEmploymentType(request.employmentType());
        candidate.setStudent(request.student());
        if (!java.util.Objects.equals(previousUsername == null ? null : previousUsername.toLowerCase(Locale.ROOT),
                updatedUsername == null ? null : updatedUsername.toLowerCase(Locale.ROOT))
                && candidate.getGithubData() != null) {
            githubDataRepository.delete(candidate.getGithubData());
            candidate.setGithubData(null);
        }
        return AuthenticatedUserResponseDTO.from(userRepository.save(candidate));
    }

    private String normalizeOptionalProfileText(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(fieldName + " must not exceed 100 characters");
        }
        return trimmed;
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String requireName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.codePoints().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException(fieldName + " must not contain digits");
        }
        return trimmed;
    }

    private String normalizeGithubProfileUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (GithubProfileUrlParser.extractUsername(trimmed).isEmpty()) {
            throw new IllegalArgumentException("GitHub profile URL is invalid");
        }
        return trimmed;
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
