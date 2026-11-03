package com.barterlink.notifications.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.notifications.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "notification_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "recipient_id",
            nullable = false
    )
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (isRead == null) {
            isRead = false;
        }
    }
}