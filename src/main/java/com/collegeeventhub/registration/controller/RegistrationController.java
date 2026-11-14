package com.collegeeventhub.registration.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.registration.dto.RegistrationResponse;
import com.collegeeventhub.registration.service.RegistrationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/events/{eventId}/registrations")
    public ResponseEntity<ApiResponse<RegistrationResponse>> registerForEvent(
            @PathVariable Long eventId,
            Authentication authentication) {

        String username = authentication.getName();

        RegistrationResponse response = registrationService.registerForEvent(
                eventId,
                username
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Registered for event successfully",
                        response
                ));
    }

    @DeleteMapping("/events/{eventId}/registrations")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(
            @PathVariable Long eventId,
            Authentication authentication) {

        String username = authentication.getName();

        registrationService.cancelRegistration(eventId, username);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Registration cancelled successfully",
                        null
                )
        );
    }

    @GetMapping("/registrations/me")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getMyRegistrations(
            Authentication authentication) {

        String username = authentication.getName();

        List<RegistrationResponse> registrations =
                registrationService.getMyRegistrations(username);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Registrations fetched successfully",
                        registrations
                )
        );
    }

    @GetMapping("/events/{eventId}/registrations")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getEventRegistrations(
            @PathVariable Long eventId,
            Authentication authentication) {

        String username = authentication.getName();

        List<RegistrationResponse> registrations =
                registrationService.getEventRegistrations(
                        eventId,
                        username
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Event registrations fetched successfully",
                        registrations
                )
        );
    }
}
