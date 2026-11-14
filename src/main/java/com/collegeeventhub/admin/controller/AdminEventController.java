package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.dto.AdminEventResponse;
import com.collegeeventhub.admin.service.AdminEventService;
import com.collegeeventhub.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminEventResponse>>> getAllEvents() {

        List<AdminEventResponse> events = adminEventService.getAllEvents();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Events fetched successfully", events)
        );
    }

    @PutMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelEvent(
            @PathVariable Long eventId) {

        adminEventService.cancelEvent(eventId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Event cancelled successfully", null)
        );
    }
}
