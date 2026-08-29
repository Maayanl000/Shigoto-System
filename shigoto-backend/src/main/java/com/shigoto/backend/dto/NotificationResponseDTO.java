package com.shigoto.backend.dto;

import com.shigoto.backend.entity.Notification;
import com.shigoto.backend.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponseDTO(Long notificationId, NotificationType type, String title,
        String message, Long applicationId, Long interviewId, LocalDateTime createdAt, boolean read) {
    public static NotificationResponseDTO from(Notification notification) {
        return new NotificationResponseDTO(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getMessage(), notification.getApplicationId(), notification.getInterviewId(),
                notification.getCreatedAt(), notification.getReadAt() != null);
    }
}
