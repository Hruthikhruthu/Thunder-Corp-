package com.thundercore.erp.notification.service;

import com.thundercore.erp.auth.entity.User;
import com.thundercore.erp.auth.repository.UserRepository;
import com.thundercore.erp.notification.entity.Notification;
import com.thundercore.erp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * NotificationService persists alerts and pushes realtime user messages.
 *
 * <p>Inventory low-stock and finance overdue flows call this service to notify
 * active managers and administrators. Saved notifications are also sent over
 * /user/queue/notifications for live bell updates in the React shell.</p>
 */
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:test@example.com}")
    private String fromAddress;

    @Transactional
    /**
     * Creates one notification for a user and pushes it to that user's STOMP
     * queue.
     *
     * @param userId recipient user id
     * @param title short notification title
     * @param message detailed notification message
     * @return persisted notification
     */
    public Notification sendNotification(Long userId, String title, String message) {
        User user = userRepository.findById(userId).orElseThrow();
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        Notification saved = notificationRepository.save(notification);
        
        // Push to the authenticated user's STOMP queue for the notification bell.
        messagingTemplate.convertAndSendToUser(
                user.getEmail(), 
                "/queue/notifications", 
                saved
        );
        sendEmailAlert(user.getEmail(), title, message);
        
        return saved;
    }

    @Transactional
    /**
     * Sends the same alert to every active user in the supplied roles.
     *
     * @param roles role names without ROLE_ prefix
     * @param title short notification title
     * @param message detailed notification message
     */
    public void sendNotificationToRoles(Set<String> roles, String title, String message) {
        userRepository.findByRoleInAndActiveTrue(roles).forEach(user ->
                sendNotification(user.getId(), title, message)
        );
    }

    /** Returns newest-first notifications for the requested user. */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Counts unread notifications for badge display. */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    /** Marks a notification as read without ownership filtering. */
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow();
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    /** Marks a notification as read only when it belongs to the user. */
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId).orElseThrow();
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    /** Marks all unread notifications for a user as read. */
    public void markAllAsRead(Long userId) {
        notificationRepository.findByUserIdAndIsReadFalse(userId).forEach(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    private void sendEmailAlert(String to, String title, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromAddress);
            email.setTo(to);
            email.setSubject("ThunderCore ERP - " + title);
            email.setText(message);
            mailSender.send(email);
        } catch (Exception ex) {
            log.info("Email alert skipped for {}: {}", to, ex.getMessage());
        }
    }
}
