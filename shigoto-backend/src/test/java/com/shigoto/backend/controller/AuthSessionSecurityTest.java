package com.shigoto.backend.controller;

import com.shigoto.backend.config.SecurityConfig;
import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.LoginRequestDTO;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.repository.UserRepository;
import com.shigoto.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
class AuthSessionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void successfulLoginRotatesSessionAndAuthenticatedSessionCanReadMe() throws Exception {
        MockHttpSession preAuthenticationSession = new MockHttpSession();
        String originalSessionId = preAuthenticationSession.getId();

        MockHttpSession authenticatedSession = login(preAuthenticationSession);

        assertNotEquals(originalSessionId, authenticatedSession.getId());
        assertNotNull(authenticatedSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("candidate@example.com"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
    }

    @Test
    void logoutInvalidatesAuthenticatedSessionAndOldIdCannotReadMe() throws Exception {
        MockHttpSession authenticatedSession = login(new MockHttpSession());
        String authenticatedSessionId = authenticatedSession.getId();

        mockMvc.perform(post("/api/auth/logout").session(authenticatedSession).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertTrue(authenticatedSession.isInvalid());
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("JSESSIONID", authenticatedSessionId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void failedLoginDoesNotCreateAuthenticatedSession() throws Exception {
        when(authService.authenticate(any(LoginRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        MvcResult failedLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNull(failedLogin.getRequest().getSession(false));
        verify(authService, never()).getAuthenticatedUser(any());
    }

    private MockHttpSession login(MockHttpSession initialSession) throws Exception {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "candidate@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")));
        AuthenticatedUserResponseDTO response = new AuthenticatedUserResponseDTO(
                2L, "Maya", "Levi", "candidate@example.com", Role.CANDIDATE,
                null, null, null, null, null, false);
        when(authService.authenticate(any(LoginRequestDTO.class))).thenReturn(authentication);
        when(authService.getAuthenticatedUser(any(Authentication.class))).thenReturn(response);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .session(initialSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("candidate@example.com"))
                .andReturn();

        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private String loginJson() {
        return """
                {
                  "email": "candidate@example.com",
                  "password": "secret123"
                }
                """;
    }
}
