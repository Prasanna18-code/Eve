package com.collegeeventhub.registration.service;

import com.collegeeventhub.registration.dto.RegistrationResponse;

import java.util.List;

public interface RegistrationService {

    RegistrationResponse registerForEvent(
            Long eventId,
            String username
    );

    void cancelRegistration(
            Long eventId,
            String username
    );

    List<RegistrationResponse> getMyRegistrations(
            String username
    );

    List<RegistrationResponse> getEventRegistrations(
            Long eventId,
            String username
    );
}
