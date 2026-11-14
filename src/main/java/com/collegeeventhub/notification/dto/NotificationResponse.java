package com.collegeeventhub.notification.dto;

import com.collegeeventhub.common.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private String title;

    private String message;

    private NotificationType type;

    private Boolean read;

    private LocalDateTime createdAt;
}
