package com.shigoto.backend.service;

import com.shigoto.backend.entity.Notification;
import com.shigoto.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes candidate events, persists idempotent notifications, and exposes candidate-owned notification state.
 * Required collaborators are supplied through Lombok-generated constructor injection.
 */
@Service
@RequiredArgsConstructor
public class NotificationWriter {
    private final NotificationRepository notificationRepository;

    /**
     * Persists a candidate notification in an independent transaction.
     * @param notification the notification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Notification notification) {
        notificationRepository.saveAndFlush(notification);
    }
}
