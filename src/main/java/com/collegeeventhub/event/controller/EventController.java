package com.collegeeventhub.event.controller;

import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.event.dto.EventRequest;
import com.collegeeventhub.event.dto.EventResponse;
import com.collegeeventhub.event.service.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/organizations/{organizationId}/events")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @PathVariable Long organizationId,
            Authentication authentication,
            @Valid @RequestBody EventRequest request) {

        String username = authentication.getName();

        EventResponse response = eventService.createEvent(
                organizationId,
                username,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Event created successfully",
                        response
                ));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> getAllEvents(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Long organizationId) {
        Pagination.validate(page, size);
        PageResponse<EventResponse> events =
                eventService.getAllEvents(page, size, type, organizationId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Events retrieved successfully",
                        events
                )
        );
    }

    @GetMapping("/events/cancelled")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> getCancelledEvents(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Long organizationId) {
        Pagination.validate(page, size);
        PageResponse<EventResponse> events =
                eventService.getCancelledEvents(page, size, type, organizationId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Cancelled events retrieved successfully",
                        events
                )
        );
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(
            @PathVariable Long id) {

        EventResponse response = eventService.getEventById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Event retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/organizations/{organizationId}/events")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>>
    getEventsByOrganization(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) EventType type) {
        Pagination.validate(page, size);
        PageResponse<EventResponse> events = eventService.getEventsByOrganization(
                organizationId,
                page,
                size,
                type
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Organization events retrieved successfully",
                        events
                )
        );
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody EventRequest request) {

        String username = authentication.getName();

        EventResponse response = eventService.updateEvent(
                id,
                username,
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Event updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();

        eventService.deleteEvent(id, username);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Event deleted successfully",
                        null
                )
        );
    }

    @PutMapping("/events/{eventId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelEvent(
            @PathVariable Long eventId,
            Authentication authentication) {

        eventService.cancelEvent(
                eventId,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Event cancelled successfully",
                        null
                )
        );
    }
}
