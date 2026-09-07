package com.shigoto.backend.repository;

import com.shigoto.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Declares application-specific persistence queries for notification data.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * Lists a recipient's notifications from newest to oldest.
     * @param recipientId the recipient id
     * @return recipient-owned notifications ordered by creation time descending
     */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    /**
     * Finds a notification only when it belongs to the supplied recipient.
     * @param id the entity identifier
     * @param recipientId the recipient id
     * @return the recipient-owned notification, if present
     */
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
    /**
     * Checks whether a notification has already been persisted for an event.
     * @param eventId the event id
     * @return {@code true} when the event already has a notification; otherwise {@code false}
     */
    boolean existsByEventId(UUID eventId);
}
