package com.collegeeventhub.notification.mapper;

import com.collegeeventhub.notification.dto.NotificationResponse;
import com.collegeeventhub.notification.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(NotificationEntity entity) {

        return NotificationResponse.builder()
                .id(entity.getId())
                .eventId(entity.getEvent() == null ? null : entity.getEvent().getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .read(entity.getRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
