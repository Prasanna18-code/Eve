package com.collegeeventhub.notification.service;

import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.dto.NotificationPageResponse;
import com.collegeeventhub.notification.dto.NotificationResponse;
import com.collegeeventhub.notification.entity.NotificationEntity;
import com.collegeeventhub.notification.entity.NotificationPreferenceEntity;
import com.collegeeventhub.notification.mapper.NotificationMapper;
import com.collegeeventhub.notification.repository.NotificationPreferenceRepository;
import com.collegeeventhub.notification.repository.NotificationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserEntity mockUser(Long id) {
        return UserEntity.builder()
                .id(id)
                .username("student" + id)
                .build();
    }

    private NotificationEntity mockNotification(UserEntity user) {
        return NotificationEntity.builder()
                .user(user)
                .read(false)
                .build();
    }

    @Test
    void getMyNotificationsReturnsPageForAuthenticatedUserWithStableSort() {
        UserEntity user = mockUser(1L);
        NotificationEntity notification = mockNotification(user);
        NotificationResponse response = mock(NotificationResponse.class);
        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<NotificationEntity> notificationPage =
                new PageImpl<>(List.of(notification), pageable, 21);

        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(notificationRepository.findByUserId(1L, pageable))
                .thenReturn(notificationPage);
        when(notificationMapper.toResponse(notification))
                .thenReturn(response);

        NotificationPageResponse result =
                notificationService.getMyNotifications("student1", 0, 10);

        assertEquals(List.of(response), result.getContent());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(21, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertFalse(result.isLast());
        verify(notificationRepository).findByUserId(1L, pageable);
        verifyNoMoreInteractions(notificationRepository);
        verify(notificationMapper).toResponse(notification);
    }

    @Test
    void getMyNotificationsThrowsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.getMyNotifications("missing", 0, 10)
        );

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void getMyNotificationsUsesOnlyTheAuthenticatedUsersId() {
        UserEntity user = mockUser(1L);
        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(notificationRepository.findByUserId(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        notificationService.getMyNotifications("student1", 2, 5);

        verify(notificationRepository).findByUserId(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Pageable.class)
        );
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void getUnreadCountReturnsRepositoryCount() {
        UserEntity user = mockUser(1L);

        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(notificationRepository.countByUserIdAndReadFalse(1L))
                .thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount("student1"));
    }

    @Test
    void markAsReadMarksOwnedNotificationAsRead() {
        UserEntity user = mockUser(1L);
        NotificationEntity notification = mockNotification(user);

        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(notificationRepository.findById(10L))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(10L, "student1");

        assertTrue(notification.getRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadRejectsAnotherUsersNotification() {
        UserEntity currentUser = mockUser(1L);
        UserEntity notificationOwner = mockUser(2L);
        NotificationEntity notification = mockNotification(notificationOwner);

        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(10L))
                .thenReturn(Optional.of(notification));

        assertThrows(
                AccessDeniedException.class,
                () -> notificationService.markAsRead(10L, "student1")
        );

        verify(notificationRepository, never()).save(any());
        assertFalse(notification.getRead());
    }

    @Test
    void markAsReadThrowsWhenNotificationDoesNotExist() {
        UserEntity user = mockUser(1L);

        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(notificationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.markAsRead(99L, "student1")
        );
    }

    @Test
    void markAllAsReadMarksAndSavesAllNotifications() {
        UserEntity user = mockUser(1L);
        NotificationEntity first = mockNotification(user);
        NotificationEntity second = mockNotification(user);
        List<NotificationEntity> notifications = List.of(first, second);

        when(userRepository.findByUsername("student1"))
                .thenReturn(Optional.of(user));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(notifications);

        notificationService.markAllAsRead("student1");

        assertTrue(first.getRead());
        assertTrue(second.getRead());
        verify(notificationRepository).saveAll(notifications);
    }

    @Test
    void createNotificationWithoutEventSavesUnreadNotification() {
        UserEntity user = mockUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationService.createNotification(
                1L,
                "Welcome",
                "Welcome to the college event hub",
                NotificationType.REGISTRATION_SUCCESSFUL
        );

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());

        NotificationEntity saved = captor.getValue();
        assertSame(user, saved.getUser());
        assertNull(saved.getEvent());
        assertEquals("Welcome", saved.getTitle());
        assertEquals(
                "Welcome to the college event hub",
                saved.getMessage()
        );
        assertEquals(
                NotificationType.REGISTRATION_SUCCESSFUL,
                saved.getType()
        );
        assertFalse(saved.getRead());
        verifyNoInteractions(eventRepository);
        verify(preferenceRepository).findByUserId(1L);
    }

    @Test
    void createNotificationWithEventSavesNotification() {
        UserEntity user = mockUser(1L);
        EventEntity event = mock(EventEntity.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));

        notificationService.createNotification(
                1L,
                20L,
                "Event updated",
                "The event details changed",
                NotificationType.EVENT_UPDATED
        );

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());

        NotificationEntity saved = captor.getValue();
        assertSame(user, saved.getUser());
        assertSame(event, saved.getEvent());
        assertEquals("Event updated", saved.getTitle());
        assertEquals(
                "The event details changed",
                saved.getMessage()
        );
        assertEquals(NotificationType.EVENT_UPDATED, saved.getType());
        assertFalse(saved.getRead());
        assertSame(event, saved.getEvent());
        verify(preferenceRepository).findByUserId(1L);
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void createNotificationSkipsWhenTypeIsDisabled(NotificationType type) {
        UserEntity user = mockUser(1L);
        NotificationPreferenceEntity preferences = disabledPreference(type);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(preferences));

        notificationService.createNotification(
                1L,
                "Title",
                "Message",
                type
        );

        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(eventRepository);
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void createNotificationSavesWhenTypeIsEnabled(NotificationType type) {
        UserEntity user = mockUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.of(enabledPreference()));

        notificationService.createNotification(
                1L,
                "Title",
                "Message",
                type
        );

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertSame(user, captor.getValue().getUser());
        assertEquals(type, captor.getValue().getType());
        assertFalse(captor.getValue().getRead());
        verifyNoInteractions(eventRepository);
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void createNotificationWithoutPreferenceRecordRemainsEnabled(
            NotificationType type) {
        UserEntity user = mockUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        notificationService.createNotification(
                1L,
                "Title",
                "Message",
                type
        );

        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(1L);
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void createNotificationThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.createNotification(
                        99L,
                        "Title",
                        "Message",
                        NotificationType.REGISTRATION_SUCCESSFUL
                )
        );

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotificationThrowsWhenEventDoesNotExist() {
        UserEntity user = mockUser(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.createNotification(
                        1L,
                        99L,
                        "Title",
                        "Message",
                        NotificationType.EVENT_UPDATED
                )
        );

        verify(notificationRepository, never()).save(any());
    }

    private NotificationPreferenceEntity enabledPreference() {
        return NotificationPreferenceEntity.builder()
                .registrationSuccessful(true)
                .eventUpdated(true)
                .eventCancelled(true)
                .eventReminder(true)
                .build();
    }

    private NotificationPreferenceEntity disabledPreference(
            NotificationType type) {
        return NotificationPreferenceEntity.builder()
                .registrationSuccessful(
                        type != NotificationType.REGISTRATION_SUCCESSFUL
                )
                .eventUpdated(type != NotificationType.EVENT_UPDATED)
                .eventCancelled(type != NotificationType.EVENT_CANCELLED)
                .eventReminder(type != NotificationType.EVENT_REMINDER)
                .build();
    }
}
