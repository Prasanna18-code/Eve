package com.collegeeventhub.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {

    @NotNull
    private Boolean registrationSuccessful;

    @NotNull
    private Boolean eventUpdated;

    @NotNull
    private Boolean eventCancelled;

    @NotNull
    private Boolean eventReminder;
}
