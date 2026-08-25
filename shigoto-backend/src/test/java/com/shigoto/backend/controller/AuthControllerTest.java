package com.shigoto.backend.controller;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.LoginRequestDTO;
import com.shigoto.backend.entity.Role;
import com.shigoto.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void successfulLoginStoresSecurityContextInHttpSession() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        LoginRequestDTO request = new LoginRequestDTO("candidate@example.com", "secret123");
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "candidate@example.com", null, List.of());
        AuthenticatedUserResponseDTO user = new AuthenticatedUserResponseDTO(
                2L, "Maya", "Levi", "candidate@example.com", Role.CANDIDATE, null);
        when(authService.authenticate(request)).thenReturn(authentication);
        when(authService.getAuthenticatedUser(authentication)).thenReturn(user);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        var response = controller.login(request, httpRequest, httpResponse);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(user, response.getBody());
        assertNotNull(httpRequest.getSession(false));
        assertNotNull(httpRequest.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }
}
