package com.collegeeventhub.notification.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.notification.dto.NotificationPreferenceRequest;
import com.collegeeventhub.notification.dto.NotificationPreferenceResponse;
import com.collegeeventhub.notification.entity.NotificationPreferenceEntity;
import com.collegeeventhub.notification.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(
            Authentication authentication) {

        NotificationPreferenceResponse response = toResponse(
                preferenceService.getPreferences(authentication.getName())
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notification preferences fetched successfully", response)
        );
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request,
            Authentication authentication) {

        NotificationPreferenceResponse response = toResponse(
                preferenceService.updatePreferences(
                        authentication.getName(),
                        request.getRegistrationSuccessful(),
                        request.getEventUpdated(),
                        request.getEventCancelled(),
                        request.getEventReminder()
                )
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notification preferences updated successfully", response)
        );
    }

    private NotificationPreferenceResponse toResponse(
            NotificationPreferenceEntity preferences) {
        return NotificationPreferenceResponse.builder()
                .registrationSuccessful(preferences.isRegistrationSuccessful())
                .eventUpdated(preferences.isEventUpdated())
                .eventCancelled(preferences.isEventCancelled())
                .eventReminder(preferences.isEventReminder())
                .build();
    }
}
