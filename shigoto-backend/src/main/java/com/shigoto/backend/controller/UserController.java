package com.shigoto.backend.controller;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes user HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Registers a candidate through the legacy user endpoint.
     * @param request the request payload
     * @return an HTTP response containing the registered candidate
     */
    @Deprecated
    @PostMapping("/register")
    public ResponseEntity<AuthenticatedUserResponseDTO> registerUser(
            @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerCandidate(request));
    }
}
