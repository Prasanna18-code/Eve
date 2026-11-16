package com.collegeeventhub.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private boolean registrationSuccessful;

    private boolean eventUpdated;

    private boolean eventCancelled;

    private boolean eventReminder;
}
