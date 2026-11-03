package com.barterlink.notifications.repository;

import com.barterlink.auth.entities.User;
import com.barterlink.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(
            User recipient);

    List<Notification> findByRecipientAndIsReadOrderByCreatedAtDesc(
            User recipient,
            Boolean isRead);

    long countByRecipientAndIsRead(
            User recipient,
            Boolean isRead);
}