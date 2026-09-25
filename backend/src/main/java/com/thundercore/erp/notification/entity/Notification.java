package com.thundercore.erp.notification.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thundercore.erp.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Notification is a persisted user-facing alert.
 *
 * <p>Records are stored for inbox history and also delivered live over STOMP
 * when created by inventory, finance, or other operational workflows.</p>
 */
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Recipient relationship; hidden from JSON to avoid lazy-loading cycles. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private Boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
