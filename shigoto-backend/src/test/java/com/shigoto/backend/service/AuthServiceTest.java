package com.shigoto.backend.service;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.CandidateProfileUpdateRequestDTO;
import com.shigoto.backend.dto.LoginRequestDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.entity.EmploymentType;
import com.shigoto.backend.entity.Company;
import com.shigoto.backend.entity.User;
import com.shigoto.backend.exception.DuplicateEmailException;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.repository.GithubDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;

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
    private GithubDataRepository githubDataRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authenticationManager = mock(AuthenticationManager.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        githubDataRepository = mock(GithubDataRepository.class);
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, githubDataRepository);
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
                new RegisterRequestDTO("  Maya ", " Levi  ", " Candidate@Example.COM ", "secret123",
                        " https://github.com/maya "));

        assertEquals(7L, response.id());
        assertEquals("Maya", response.firstName());
        assertEquals("Levi", response.lastName());
        assertEquals("candidate@example.com", response.email());
        assertEquals(Role.CANDIDATE, response.role());
        assertEquals(Role.CANDIDATE, saved.get().getRole());
        assertEquals("https://github.com/maya", saved.get().getGithubProfileUrl());
        assertEquals("https://github.com/maya", response.githubProfileUrl());
        assertNotEquals("secret123", saved.get().getPassword());
        assertTrue(passwordEncoder.matches("secret123", saved.get().getPassword()));
    }

    @Test
    void publicRegistrationCannotCreateAStaffRole() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.registerCandidate(
                new RegisterRequestDTO("Dana", "Cohen", "dana@example.com", "secret123",
                        "https://github.com/dana"));

        assertEquals(Role.CANDIDATE, response.role());
        verify(userRepository).save(argThat(user -> user.getRole() == Role.CANDIDATE));
        assertFalse(Arrays.stream(RegisterRequestDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("role")));
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.registerCandidate(
                new RegisterRequestDTO("Dana", "Cohen", " Duplicate@Example.com ", "secret123",
                        "https://github.com/dana")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void boundedRegistrationStringsAccept255AndReject256Characters() {
        String maximumName = "a".repeat(255);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var accepted = authService.registerCandidate(new RegisterRequestDTO(
                maximumName, "Cohen", "max@example.com", "secret123", "https://github.com/max"));
        assertEquals(255, accepted.firstName().length());

        assertThrows(IllegalArgumentException.class, () -> authService.registerCandidate(
                new RegisterRequestDTO("a".repeat(256), "Cohen", "long-name@example.com", "secret123",
                        "https://github.com/longname")));
        assertThrows(IllegalArgumentException.class, () -> authService.registerCandidate(
                new RegisterRequestDTO("Dana", "Cohen", "a".repeat(244) + "@example.com", "secret123",
                        "https://github.com/longemail")));
    }

    @Test
    void registrationMapsOnlyActualDuplicateEmailIntegrityFailuresToConflict() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Dana", "Cohen", "race@example.com", "secret123", "https://github.com/dana");
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false, true);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("email unique constraint"));
        assertThrows(DuplicateEmailException.class, () -> authService.registerCandidate(request));

        reset(userRepository);
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false, false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unrelated constraint"));
        assertThrows(DataIntegrityViolationException.class, () -> authService.registerCandidate(request));
    }

    @Test
    void registrationRequiresValidGithubProfile() {
        assertThrows(IllegalArgumentException.class, () -> authService.registerCandidate(
                new RegisterRequestDTO("Dana", "Cohen", "dana@example.com", "secret123", "")));
        assertThrows(IllegalArgumentException.class, () -> authService.registerCandidate(
                new RegisterRequestDTO("Dana", "Cohen", "dana@example.com", "secret123",
                        "https://example.com/dana")));
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
                .email("dana@example.com").password("encoded").role(Role.CANDIDATE)
                .githubProfileUrl("https://github.com/dana").build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "dana@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("dana@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUserResponseDTO response = authService.getAuthenticatedUser(authentication);

        assertEquals(9L, response.id());
        assertEquals("dana@example.com", response.email());
        assertEquals(Role.CANDIDATE, response.role());
        assertEquals("https://github.com/dana", response.githubProfileUrl());
    }

    @Test
    void includesStaffCompanyNameInAuthenticatedUserResponse() {
        Company company = Company.builder().id(4L).name("Wix").build();
        User hr = User.builder().id(10L).firstName("Maayan").lastName("Lahyani")
                .email("maayan@wix.com").role(Role.HR).company(company).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "maayan@wix.com", null, java.util.List.of());
        when(userRepository.findByEmail("maayan@wix.com")).thenReturn(Optional.of(hr));

        AuthenticatedUserResponseDTO response = authService.getAuthenticatedUser(authentication);

        assertEquals("Wix", response.companyName());
    }

    @Test
    void authenticatedCandidateUpdatesProfileWithoutSupplyingUserId() {
        User candidate = User.builder().id(9L).firstName("Old").lastName("Name")
                .email("dana@example.com").role(Role.CANDIDATE).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "dana@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("dana@example.com")).thenReturn(Optional.of(candidate));
        when(userRepository.save(candidate)).thenReturn(candidate);
        CandidateProfileUpdateRequestDTO request = new CandidateProfileUpdateRequestDTO(
                " Dana ", " Cohen ", " https://github.com/dana ",
                " Junior Java Developer ", " Backend Developer ", EmploymentType.STUDENT, false);

        var response = authService.updateCandidateProfile(request, authentication);

        assertEquals(9L, response.id());
        assertEquals("Dana", response.firstName());
        assertEquals("Cohen", response.lastName());
        assertEquals("https://github.com/dana", response.githubProfileUrl());
        assertEquals("Junior Java Developer", response.currentTitle());
        assertEquals("Backend Developer", response.desiredRole());
        assertEquals(EmploymentType.STUDENT, response.employmentType());
        assertFalse(response.student());
        verify(userRepository).save(candidate);
        assertFalse(Arrays.stream(CandidateProfileUpdateRequestDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("id")));
    }

    @Test
    void authenticatedCandidateCanClearEmploymentPreferenceIndependentlyOfStudentStatus() {
        User candidate = User.builder().id(9L).firstName("Dana").lastName("Cohen")
                .email("dana@example.com").role(Role.CANDIDATE)
                .employmentType(EmploymentType.FULL_TIME).student(false).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "dana@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("dana@example.com")).thenReturn(Optional.of(candidate));
        when(userRepository.save(candidate)).thenReturn(candidate);
        CandidateProfileUpdateRequestDTO request = new CandidateProfileUpdateRequestDTO(
                "Dana", "Cohen", "https://github.com/dana",
                null, null, null, true);

        var response = authService.updateCandidateProfile(request, authentication);

        assertNull(response.employmentType());
        assertTrue(response.student());
        assertNull(candidate.getEmploymentType());
        assertTrue(candidate.isStudent());
        verify(userRepository).save(candidate);
    }

    @Test
    void rejectsInvalidGithubProfileAndDigitsInNames() {
        User candidate = User.builder().id(9L).email("dana@example.com").role(Role.CANDIDATE).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "dana@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("dana@example.com")).thenReturn(Optional.of(candidate));

        assertThrows(IllegalArgumentException.class, () -> authService.updateCandidateProfile(
                new CandidateProfileUpdateRequestDTO("Dana", "Cohen", "https://example.com/dana",
                        null, null, EmploymentType.FULL_TIME, false),
                authentication));
        assertThrows(IllegalArgumentException.class, () -> authService.updateCandidateProfile(
                new CandidateProfileUpdateRequestDTO("Dana2", "Cohen", "https://github.com/dana",
                        null, null, EmploymentType.FULL_TIME, false),
                authentication));
        verify(userRepository, never()).save(candidate);
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
    void resolvesAuthenticatedHrWithCompany() {
        Company company = Company.builder().id(4L).name("Shigoto").build();
        User hr = User.builder().id(10L).email("hr@example.com").role(Role.HR).company(company).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "hr@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(hr));

        assertSame(hr, authService.getAuthenticatedHr(authentication));
    }

    @Test
    void authenticatedHrWithoutCompanyIsRejected() {
        User hr = User.builder().id(10L).email("hr@example.com").role(Role.HR).build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "hr@example.com", null, java.util.List.of());
        when(userRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(hr));

        assertThrows(AccessDeniedException.class, () -> authService.getAuthenticatedHr(authentication));
    }

    @Test
    void safeResponseHasNoPasswordComponent() {
        assertTrue(Arrays.stream(AuthenticatedUserResponseDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("githubProfileUrl")));
        assertTrue(Arrays.stream(AuthenticatedUserResponseDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("employmentType")));
        assertTrue(Arrays.stream(AuthenticatedUserResponseDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("student")));
        assertFalse(Arrays.stream(AuthenticatedUserResponseDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("password")));
    }
}
