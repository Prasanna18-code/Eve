package com.collegeeventhub.notification.service;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.notification.entity.NotificationPreferenceEntity;
import com.collegeeventhub.notification.repository.NotificationPreferenceRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl
        implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceEntity getPreferences(String username) {
        UserEntity user = getUser(username);

        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> NotificationPreferenceEntity.builder().build());
    }

    @Override
    @Transactional
    public NotificationPreferenceEntity updatePreferences(
            String username,
            boolean registrationSuccessful,
            boolean eventUpdated,
            boolean eventCancelled,
            boolean eventReminder) {

        UserEntity user = getUser(username);
        NotificationPreferenceEntity preferences =
                preferenceRepository.findByUserId(user.getId())
                        .orElseGet(() -> NotificationPreferenceEntity.builder()
                                .user(user)
                                .build());

        preferences.setRegistrationSuccessful(registrationSuccessful);
        preferences.setEventUpdated(eventUpdated);
        preferences.setEventCancelled(eventCancelled);
        preferences.setEventReminder(eventReminder);

        return preferenceRepository.save(preferences);
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username
                        ));
    }
}
