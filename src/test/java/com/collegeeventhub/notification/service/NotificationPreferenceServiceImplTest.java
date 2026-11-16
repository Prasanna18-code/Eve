package com.collegeeventhub.notification.service;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.notification.entity.NotificationPreferenceEntity;
import com.collegeeventhub.notification.repository.NotificationPreferenceRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferenceServiceImpl preferenceService;

    @Test
    void getPreferencesReturnsAllEnabledDefaultsWithoutCreatingRecord() {
        UserEntity user = user(1L);
        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        NotificationPreferenceEntity result =
                preferenceService.getPreferences("student1");

        assertTrue(result.isRegistrationSuccessful());
        assertTrue(result.isEventUpdated());
        assertTrue(result.isEventCancelled());
        assertTrue(result.isEventReminder());
        assertNull(result.getId());
        verify(preferenceRepository, never()).save(result);
    }

    @Test
    void getPreferencesReturnsSavedRecordForAuthenticatedUser() {
        UserEntity user = user(1L);
        NotificationPreferenceEntity preferences =
                NotificationPreferenceEntity.builder()
                        .id(20L)
                        .user(user)
                        .eventUpdated(false)
                        .build();
        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(preferences));

        NotificationPreferenceEntity result =
                preferenceService.getPreferences("student1");

        assertSame(preferences, result);
        assertFalse(result.isEventUpdated());
    }

    @Test
    void updatePreferencesCreatesRecordForAuthenticatedUser() {
        UserEntity user = user(1L);
        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceEntity result = preferenceService.updatePreferences(
                "student1",
                true,
                false,
                true,
                false
        );

        assertSame(user, result.getUser());
        assertTrue(result.isRegistrationSuccessful());
        assertFalse(result.isEventUpdated());
        assertTrue(result.isEventCancelled());
        assertFalse(result.isEventReminder());
        verify(preferenceRepository).save(result);
    }

    @Test
    void updatePreferencesUpdatesExistingRecord() {
        UserEntity user = user(1L);
        NotificationPreferenceEntity existing =
                NotificationPreferenceEntity.builder()
                        .id(20L)
                        .user(user)
                        .build();
        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(existing));
        when(preferenceRepository.save(existing)).thenReturn(existing);

        NotificationPreferenceEntity result = preferenceService.updatePreferences(
                "student1",
                false,
                false,
                true,
                false
        );

        assertSame(existing, result);
        assertEquals(20L, result.getId());
        assertFalse(result.isRegistrationSuccessful());
        assertFalse(result.isEventUpdated());
        assertTrue(result.isEventCancelled());
        assertFalse(result.isEventReminder());
        verify(preferenceRepository).save(existing);
    }

    @Test
    void getPreferencesRejectsUnknownAuthenticatedUser() {
        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> preferenceService.getPreferences("missing")
        );

        verifyNoInteractions(preferenceRepository);
    }

    @Test
    void updatePreferencesRejectsUnknownAuthenticatedUser() {
        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> preferenceService.updatePreferences(
                        "missing",
                        true,
                        true,
                        true,
                        true
                )
        );

        verifyNoInteractions(preferenceRepository);
    }

    private UserEntity user(Long id) {
        return UserEntity.builder()
                .id(id)
                .username("student" + id)
                .build();
    }
}
