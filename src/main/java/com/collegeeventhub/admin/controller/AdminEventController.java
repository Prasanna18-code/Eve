package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.dto.AdminEventResponse;
import com.collegeeventhub.admin.service.AdminEventService;
import com.collegeeventhub.admin.security.AdminActor;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminEventResponse>>> getAllEvents(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Long organizationId) {
        Pagination.validate(page, size);
        PageResponse<AdminEventResponse> events = adminEventService.getAllEvents(
                page,
                size,
                status,
                type,
                organizationId
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Events fetched successfully", events)
        );
    }

    @PutMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelEvent(
            @PathVariable Long eventId,
            Authentication authentication) {

        adminEventService.cancelEvent(
                eventId,
                AdminActor.getUserId(authentication)
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Event cancelled successfully", null)
        );
    }
}
