package com.shigoto.backend.controller;

import com.shigoto.backend.dto.NotificationResponseDTO;
import com.shigoto.backend.service.AuthService;
import com.shigoto.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Exposes notification HTTP operations and delegates business rules to services.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final AuthService authService;

    /**
     * Returns notifications owned by the authenticated candidate.
     * @param authentication the current Spring Security authentication
     * @return notifications owned by the candidate, newest first
     */
    @GetMapping("/mine")
    public List<NotificationResponseDTO> mine(Authentication authentication) {
        return notificationService.mine(authService.getAuthenticatedCandidate(authentication));
    }

    /**
     * Marks one candidate-owned notification as read.
     * @param notificationId the notification identifier
     * @param authentication the current Spring Security authentication
     * @return the notification with its read timestamp populated
     */
    @PutMapping("/{notificationId}/read")
    public NotificationResponseDTO markRead(@PathVariable Long notificationId, Authentication authentication) {
        return notificationService.markRead(notificationId, authService.getAuthenticatedCandidate(authentication));
    }
}
