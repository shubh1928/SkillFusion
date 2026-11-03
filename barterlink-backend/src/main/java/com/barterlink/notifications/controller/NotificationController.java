package com.barterlink.notifications.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.notifications.dto.response.NotificationResponseDto;
import com.barterlink.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private final CurrentUserUtil currentUserUtil;

    @GetMapping
    public ApiResponse<List<NotificationResponseDto>>
    getMyNotifications() {

        return ApiResponse.success(
                notificationService.getMyNotifications(
                        currentUserUtil.getCurrentUserId()));
    }

    @GetMapping("/unread")
    public ApiResponse<List<NotificationResponseDto>>
    getUnreadNotifications() {

        return ApiResponse.success(
                notificationService.getUnreadNotifications(
                        currentUserUtil.getCurrentUserId()));
    }

    @GetMapping("/unread/count")
    public ApiResponse<Long> getUnreadCount() {

        return ApiResponse.success(
                notificationService.getUnreadCount(
                        currentUserUtil.getCurrentUserId()));
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponseDto> markAsRead(
            @PathVariable UUID notificationId) {

        return ApiResponse.success(
                "Notification marked as read",
                notificationService.markAsRead(
                        notificationId,
                        currentUserUtil.getCurrentUserId()));
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {

        notificationService.markAllAsRead(
                currentUserUtil.getCurrentUserId());

        return ApiResponse.success(
                "All notifications marked as read");
    }
}