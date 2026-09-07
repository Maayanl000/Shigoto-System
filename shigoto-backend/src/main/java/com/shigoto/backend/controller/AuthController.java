package com.shigoto.backend.controller;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.CandidateProfileUpdateRequestDTO;
import com.shigoto.backend.dto.LoginRequestDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes auth HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    /**
     * Registers a candidate account and returns the created session-user representation.
     * @param request the request payload
     * @return an HTTP response containing the registered candidate
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticatedUserResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCandidate(request));
    }

    /**
     * Exposes the current CSRF token to browser clients before protected requests.
     * @param csrfToken the current request CSRF token
     * @return a map containing the CSRF header name and token value
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName()
        );
    }

    /**
     * Authenticates credentials, applies session-fixation protection, and returns the session user.
     * @param request the request payload
     * @param httpRequest the servlet request
     * @param httpResponse the servlet response
     * @return an HTTP response containing the authenticated session user
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticatedUserResponseDTO> login(
            @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication = authService.authenticate(request);
        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return ResponseEntity.ok(authService.getAuthenticatedUser(authentication));
    }

    /**
     * Returns the user represented by the current authenticated session.
     * @param authentication the current Spring Security authentication
     * @return a DTO representing the currently authenticated user
     */
    @GetMapping("/me")
    public AuthenticatedUserResponseDTO me(Authentication authentication) {
        return authService.getAuthenticatedUser(authentication);
    }

    /**
     * Updates profile fields for the authenticated candidate.
     * @param request the request payload
     * @param authentication the current Spring Security authentication
     * @return a DTO containing the candidate's persisted profile values
     */
    @PutMapping("/me/profile")
    public AuthenticatedUserResponseDTO updateProfile(
            @RequestBody CandidateProfileUpdateRequestDTO request,
            Authentication authentication) {
        return authService.updateCandidateProfile(request, authentication);
    }

    /**
     * Logs out the authenticated session and clears its security context.
     * @param authentication the current Spring Security authentication
     * @param request the request payload
     * @param response the response
     * @return an HTTP response confirming that the local session was cleared
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
