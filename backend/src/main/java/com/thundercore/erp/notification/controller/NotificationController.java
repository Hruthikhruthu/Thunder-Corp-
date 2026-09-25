package com.thundercore.erp.notification.controller;

import com.thundercore.erp.auth.entity.User;
import com.thundercore.erp.auth.repository.UserRepository;
import com.thundercore.erp.common.dto.ApiResponse;
import com.thundercore.erp.notification.entity.Notification;
import com.thundercore.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
/**
 * NotificationController exposes user notification inbox operations.
 *
 * <p>The "me" endpoints derive the user from Spring Security authentication so
 * clients cannot accidentally read or update another user's notification state.
 * The user-id endpoint is retained for administrative/demo inspection.</p>
 */
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/user/{userId}")
    /** Returns notification history for an explicit user id. */
    public ResponseEntity<ApiResponse<List<Notification>>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getUserNotifications(userId)));
    }

    @GetMapping("/me")
    /** Returns notifications belonging to the authenticated user. */
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(Authentication authentication) {
        Long userId = currentUser(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notificationService.getUserNotifications(userId)));
    }

    @GetMapping("/me/unread-count")
    /** Returns the unread badge count for the authenticated user. */
    public ResponseEntity<ApiResponse<Map<String, Long>>> getMyUnreadCount(Authentication authentication) {
        Long userId = currentUser(authentication).getId();
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", Map.of("count", notificationService.getUnreadCount(userId))));
    }

    @PatchMapping("/{id}/read")
    /** Marks one authenticated user's notification as read. */
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(id, currentUser(authentication).getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PatchMapping("/me/read-all")
    /** Marks every unread notification as read for the authenticated user. */
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(currentUser(authentication).getId());
        return ResponseEntity.ok(ApiResponse.success("Notifications marked as read", null));
    }

    /** Resolves the persisted user for the authenticated principal email. */
    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
