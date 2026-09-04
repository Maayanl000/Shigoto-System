package com.shigoto.backend.service;

import com.shigoto.backend.entity.Notification;
import com.shigoto.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationWriter {
    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Notification notification) {
        notificationRepository.saveAndFlush(notification);
    }
}
