package com.thundercore.erp.notification.repository;

import com.thundercore.erp.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * NotificationRepository supports inbox listing, unread badge counts, and
 * ownership-safe state changes.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /** Lists notifications newest-first for the notification panel. */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Finds unread notifications for bulk mark-read operations. */
    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    /** Counts unread notifications for the bell badge. */
    Long countByUserIdAndIsReadFalse(Long userId);

    /** Looks up a notification only when it belongs to the supplied user. */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
