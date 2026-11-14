package com.collegeeventhub.notification.scheduler;

import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.repository.NotificationRepository;
import com.collegeeventhub.notification.service.NotificationService;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    @Test
    void sendsReminderWhenUserHasNoExistingReminder() {
        Long eventId = 10L;
        Long userId = 1L;

        EventEntity event = mock(EventEntity.class);
        UserEntity user = mock(UserEntity.class);
        RegistrationEntity registration = mock(RegistrationEntity.class);

        when(event.getId()).thenReturn(eventId);
        when(event.getTitle()).thenReturn("Java Workshop");
        when(event.getStartTime()).thenReturn(LocalTime.of(10, 30));
        when(user.getId()).thenReturn(userId);
        when(registration.getUser()).thenReturn(user);

        when(eventRepository.findByEventDateAndStatusNotIn(
                LocalDate.now().plusDays(1),
                List.of(EventStatus.CANCELLED, EventStatus.DELETED)))
                .thenReturn(List.of(event));
        when(registrationRepository.findByEventId(eventId))
                .thenReturn(List.of(registration));
        when(notificationRepository.existsByUserIdAndEventIdAndType(
                userId, eventId, NotificationType.EVENT_REMINDER))
                .thenReturn(false);

        notificationScheduler.sendEventReminders();

        verify(notificationService).createNotification(
                userId,
                eventId,
                "Event Reminder",
                "Reminder: \"Java Workshop\" is scheduled for tomorrow at 10:30",
                NotificationType.EVENT_REMINDER
        );
    }

    @Test
    void doesNotSendDuplicateReminder() {
        Long eventId = 10L;
        Long userId = 1L;

        EventEntity event = mock(EventEntity.class);
        UserEntity user = mock(UserEntity.class);
        RegistrationEntity registration = mock(RegistrationEntity.class);

        when(event.getId()).thenReturn(eventId);
        when(user.getId()).thenReturn(userId);
        when(registration.getUser()).thenReturn(user);

        when(eventRepository.findByEventDateAndStatusNotIn(
                LocalDate.now().plusDays(1),
                List.of(EventStatus.CANCELLED, EventStatus.DELETED)))
                .thenReturn(List.of(event));
        when(registrationRepository.findByEventId(eventId))
                .thenReturn(List.of(registration));
        when(notificationRepository.existsByUserIdAndEventIdAndType(
                userId, eventId, NotificationType.EVENT_REMINDER))
                .thenReturn(true);

        notificationScheduler.sendEventReminders();

        verify(notificationService, never()).createNotification(
                anyLong(),
                anyLong(),
                anyString(),
                anyString(),
                eq(NotificationType.EVENT_REMINDER)
        );
    }

    @Test
    void doesNothingWhenThereAreNoEventsTomorrow() {
        when(eventRepository.findByEventDateAndStatusNotIn(
                LocalDate.now().plusDays(1),
                List.of(EventStatus.CANCELLED, EventStatus.DELETED)))
                .thenReturn(List.of());

        notificationScheduler.sendEventReminders();

        verifyNoInteractions(registrationRepository);
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void excludesCancelledAndDeletedEventsFromReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(eventRepository.findByEventDateAndStatusNotIn(
                tomorrow,
                List.of(EventStatus.CANCELLED, EventStatus.DELETED)))
                .thenReturn(List.of());

        notificationScheduler.sendEventReminders();

        verify(eventRepository).findByEventDateAndStatusNotIn(
                tomorrow,
                List.of(EventStatus.CANCELLED, EventStatus.DELETED));
        verifyNoInteractions(registrationRepository);
        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void doesNothingWhenEventHasNoRegistrations() {
        EventEntity event = mock(EventEntity.class);
        when(event.getId()).thenReturn(10L);

        when(eventRepository.findByEventDateAndStatusNotIn(
                LocalDate.now().plusDays(1),
                List.of(EventStatus.CANCELLED, EventStatus.DELETED)))
                .thenReturn(List.of(event));
        when(registrationRepository.findByEventId(10L))
                .thenReturn(List.of());

        notificationScheduler.sendEventReminders();

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(notificationService);
    }

    @Test
    void sendsReminderOnlyToUsersWithoutExistingReminders() {
        Long eventId = 10L;
        Long firstUserId = 1L;
        Long secondUserId = 2L;

        EventEntity event = mock(EventEntity.class);
        UserEntity firstUser = mock(UserEntity.class);
        UserEntity secondUser = mock(UserEntity.class);
        RegistrationEntity firstRegistration = mock(RegistrationEntity.class);
        RegistrationEntity secondRegistration = mock(RegistrationEntity.class);

        when(event.getId()).thenReturn(eventId);
        when(event.getTitle()).thenReturn("Spring Boot Workshop");
        when(event.getStartTime()).thenReturn(LocalTime.of(14, 0));
        when(firstUser.getId()).thenReturn(firstUserId);
        when(secondUser.getId()).thenReturn(secondUserId);
        when(firstRegistration.getUser()).thenReturn(firstUser);
        when(secondRegistration.getUser()).thenReturn(secondUser);

        when(eventRepository.findByEventDateAndStatusNotIn(
                LocalDate.now().plusDays(1),
                List.of(EventStatus.CANCELLED, EventStatus.DELETED)))
                .thenReturn(List.of(event));
        when(registrationRepository.findByEventId(eventId))
                .thenReturn(List.of(firstRegistration, secondRegistration));
        when(notificationRepository.existsByUserIdAndEventIdAndType(
                firstUserId, eventId, NotificationType.EVENT_REMINDER))
                .thenReturn(true);
        when(notificationRepository.existsByUserIdAndEventIdAndType(
                secondUserId, eventId, NotificationType.EVENT_REMINDER))
                .thenReturn(false);

        notificationScheduler.sendEventReminders();

        verify(notificationService, never()).createNotification(
                eq(firstUserId),
                eq(eventId),
                anyString(),
                anyString(),
                eq(NotificationType.EVENT_REMINDER)
        );
        verify(notificationService).createNotification(
                secondUserId,
                eventId,
                "Event Reminder",
                "Reminder: \"Spring Boot Workshop\" is scheduled for tomorrow at 14:00",
                NotificationType.EVENT_REMINDER
        );
    }
}
