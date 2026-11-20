package com.collegeeventhub.event.service;

import com.collegeeventhub.event.dto.EventRequest;
import com.collegeeventhub.event.dto.EventResponse;

import java.util.List;

public interface EventService {

    EventResponse createEvent(
            Long organizationId,
            String username,
            EventRequest request
    );

    List<EventResponse> getAllEvents();

    List<EventResponse> getCancelledEvents();

    EventResponse getEventById(Long id);

    List<EventResponse> getEventsByOrganization(
            Long organizationId
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

    void cancelEventByAdmin(Long eventId);
}
