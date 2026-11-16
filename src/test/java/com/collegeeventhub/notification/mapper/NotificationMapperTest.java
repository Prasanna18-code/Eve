package com.collegeeventhub.notification.mapper;

import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.notification.dto.NotificationResponse;
import com.collegeeventhub.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationMapperTest {

    private final NotificationMapper notificationMapper = new NotificationMapper();

    @Test
    void mapsAssociatedEventId() {
        NotificationEntity notification = NotificationEntity.builder()
                .event(EventEntity.builder().id(20L).build())
                .build();

        NotificationResponse response = notificationMapper.toResponse(notification);

        assertEquals(20L, response.getEventId());
    }

    @Test
    void mapsNullEventIdWhenNotificationHasNoEvent() {
        NotificationEntity notification = NotificationEntity.builder()
                .event(null)
                .build();

        NotificationResponse response = notificationMapper.toResponse(notification);

        assertNull(response.getEventId());
    }
}
