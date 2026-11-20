package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.event.dto.EventRequest;
import com.collegeeventhub.event.dto.EventResponse;

public interface EventService {

    EventResponse createEvent(
            Long organizationId,
            String username,
            EventRequest request
    );

    PageResponse<EventResponse> getAllEvents(
            int page,
            int size,
            EventType type,
            Long organizationId
    );

    PageResponse<EventResponse> getCancelledEvents(
            int page,
            int size,
            EventType type,
            Long organizationId
    );

    EventResponse getEventById(Long id);

    PageResponse<EventResponse> getEventsByOrganization(
            Long organizationId,
            int page,
            int size,
            EventType type
    );

    EventResponse updateEvent(
            Long eventId,
            String username,
            EventRequest request
    );

    void deleteEvent(
            Long eventId,
            String username
    );

    void cancelEvent(
            Long eventId,
            String username
    );

    EventStatus cancelEventByAdmin(Long eventId);
}
