package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Notification;
import com.shigoto.backend.entity.NotificationType;
import java.time.LocalDateTime;

/**
 * Represents a candidate notification and its read state.
 */
public record NotificationResponseDTO(Long notificationId, NotificationType type, String title,
        String message, Long applicationId, Long interviewId, LocalDateTime createdAt, boolean read) {
    /**
     * Maps the supplied domain entity into this API response representation.
     * @param notification the notification
     * @return a DTO populated from the supplied domain entity
     */
    public static NotificationResponseDTO from(Notification notification) {
        return new NotificationResponseDTO(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getMessage(), notification.getApplicationId(), notification.getInterviewId(),
                notification.getCreatedAt(), notification.getReadAt() != null);
    }
}
