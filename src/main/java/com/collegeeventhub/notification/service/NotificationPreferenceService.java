package com.collegeeventhub.notification.service;

import com.collegeeventhub.notification.entity.NotificationPreferenceEntity;

public interface NotificationPreferenceService {

    NotificationPreferenceEntity getPreferences(String username);

    NotificationPreferenceEntity updatePreferences(
            String username,
            boolean registrationSuccessful,
            boolean eventUpdated,
            boolean eventCancelled,
            boolean eventReminder
    );
}
