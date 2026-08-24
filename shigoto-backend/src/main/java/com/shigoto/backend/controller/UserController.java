package com.shigoto.backend.controller;

import com.shigoto.backend.dto.AuthenticatedUserResponseDTO;
import com.shigoto.backend.dto.RegisterRequestDTO;
import com.shigoto.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /** @deprecated Use POST /api/auth/register. This compatibility route is candidate-only. */
    @Deprecated
    @PostMapping("/register")
    public ResponseEntity<AuthenticatedUserResponseDTO> registerUser(
            @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerCandidate(request));
    }

    @GetMapping
    public ResponseEntity<List<AuthenticatedUserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
