package com.shigoto.backend.controller;

import com.shigoto.backend.dto.NotificationResponseDTO;
import com.shigoto.backend.service.AuthService;
import com.shigoto.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final AuthService authService;

    @GetMapping("/mine")
    public List<NotificationResponseDTO> mine(Authentication authentication) {
        return notificationService.mine(authService.getAuthenticatedCandidate(authentication));
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponseDTO markRead(@PathVariable Long notificationId, Authentication authentication) {
        return notificationService.markRead(notificationId, authService.getAuthenticatedCandidate(authentication));
    }
}
