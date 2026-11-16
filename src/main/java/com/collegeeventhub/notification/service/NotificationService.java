package com.collegeeventhub.notification.service;

import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.notification.dto.NotificationPageResponse;

public interface NotificationService {

    NotificationPageResponse getMyNotifications(String username, int page, int size);

    long getUnreadCount(String username);

    void markAsRead(Long notificationId, String username);

    void markAllAsRead(String username);

    void createNotification(
            Long userId,
            String title,
            String message,
            NotificationType type
    );

    void createNotification(
            Long userId,
            Long eventId,
            String title,
            String message,
            NotificationType type
    );
}
