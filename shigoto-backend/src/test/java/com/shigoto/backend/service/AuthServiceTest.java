package com.shigoto.backend.service;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.LoginRequestDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.DuplicateEmailException;
import com.shigoto.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authenticationManager = mock(AuthenticationManager.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager);
    }

    @Test
    void registersNormalizedCandidateWithEncodedPassword() {
        AtomicReference<User> saved = new AtomicReference<>();
        when(userRepository.existsByEmail("candidate@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            saved.set(user);
            return user;
        });

        AuthenticatedUserResponseDTO response = authService.registerCandidate(
                new RegisterRequestDTO("  Maya ", " Levi  ", " Candidate@Example.COM ", "secret123"));

        assertEquals(7L, response.id());
        assertEquals("Maya", response.firstName());
        assertEquals("Levi", response.lastName());
        assertEquals("candidate@example.com", response.email());
        assertEquals(Role.CANDIDATE, response.role());
        assertEquals(Role.CANDIDATE, saved.get().getRole());
        assertNotEquals("secret123", saved.get().getPassword());
        assertTrue(passwordEncoder.matches("secret123", saved.get().getPassword()));
    }

    @Test
    void publicRegistrationCannotCreateAStaffRole() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.registerCandidate(
                new RegisterRequestDTO("Dana", "Cohen", "dana@example.com", "secret123"));

        assertEquals(Role.CANDIDATE, response.role());
        verify(userRepository).save(argThat(user -> user.getRole() == Role.CANDIDATE));
        assertFalse(Arrays.stream(RegisterRequestDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("role")));
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.registerCandidate(
                new RegisterRequestDTO("Dana", "Cohen", " Duplicate@Example.com ", "secret123")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticatesWithNormalizedEmail() {
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                "candidate@example.com", null, java.util.List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);

        Authentication result = authService.authenticate(
                new LoginRequestDTO(" Candidate@Example.COM ", "secret123"));

        assertSame(authenticated, result);
        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication.getName().equals("candidate@example.com")
                        && authentication.getCredentials().equals("secret123")));
    }

    @Test
    void rejectsInvalidLogin() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(
                new LoginRequestDTO("candidate@example.com", "wrong-password")));
    }

    @Test
    void loadsCurrentUserFromAuthenticationName() {
        User user = User.builder().id(9L).firstName("Dana").lastName("Cohen")
                .email("dana@example.com").password("encoded").role(Role.CANDIDATE).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "dana@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("dana@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUserResponseDTO response = authService.getAuthenticatedUser(authentication);

        assertEquals(9L, response.id());
        assertEquals("dana@example.com", response.email());
        assertEquals(Role.CANDIDATE, response.role());
    }

    @Test
    void candidateIdentityRejectsAuthenticatedStaffUser() {
        User hr = User.builder().id(10L).email("hr@example.com").role(Role.HR).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "hr@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(hr));

        assertThrows(AccessDeniedException.class,
                () -> authService.getAuthenticatedCandidate(authentication));
    }

    @Test
    void safeResponseHasNoPasswordComponent() {
        assertFalse(Arrays.stream(AuthenticatedUserResponseDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("password")));
    }
}
