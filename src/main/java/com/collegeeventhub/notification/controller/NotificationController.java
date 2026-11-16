package com.collegeeventhub.notification.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.notification.dto.NotificationPageResponse;
import com.collegeeventhub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page index must not be negative"
            );
        }
        if (size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 50"
            );
        }

        NotificationPageResponse notifications =
                notificationService.getMyNotifications(
                        authentication.getName(),
                        page,
                        size
                );

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
