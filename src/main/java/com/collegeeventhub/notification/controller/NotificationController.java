package com.collegeeventhub.notification.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.notification.dto.NotificationResponse;
import com.collegeeventhub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            Authentication authentication) {

        List<NotificationResponse> notifications =
                notificationService.getMyNotifications(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Notifications fetched successfully",
                        notifications
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            Authentication authentication) {

        long count = notificationService.getUnreadCount(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Unread notification count fetched successfully",
                        count
                )
        );
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {

        notificationService.markAsRead(
                notificationId,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Notification marked as read",
                        null
                )
        );
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            Authentication authentication) {

        notificationService.markAllAsRead(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "All notifications marked as read",
                        null
                )
        );
    }
}
