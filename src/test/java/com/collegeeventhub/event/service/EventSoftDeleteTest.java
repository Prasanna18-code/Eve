package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.mapper.EventMapper;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.service.NotificationService;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSoftDeleteTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private StudentProfileVerificationGuard studentProfileVerificationGuard;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void leaderSoftDeletesEventAndLeavesRegistrationHistoryUntouched() {
        EventEntity event = eventLedBy("leader");
        when(eventRepository.findByIdAndStatusNot(10L, EventStatus.DELETED))
                .thenReturn(Optional.of(event));

        eventService.deleteEvent(10L, "leader");

        assertEquals(EventStatus.DELETED, event.getStatus());
        verify(eventRepository).save(event);
        verify(eventRepository, never()).delete(event);
        verifyNoInteractions(registrationRepository, notificationService);
    }

    @Test
    void nonLeaderCannotSoftDeleteEvent() {
        EventEntity event = eventLedBy("leader");
        when(eventRepository.findByIdAndStatusNot(10L, EventStatus.DELETED))
                .thenReturn(Optional.of(event));

        assertThrows(
                AccessDeniedException.class,
                () -> eventService.deleteEvent(10L, "other-user")
        );

        verify(eventRepository, never()).save(event);
        verify(eventRepository, never()).delete(event);
    }

    @Test
    void deletedEventIsNotReturnedByEventDetails() {
        when(eventRepository.findByIdAndStatusNot(10L, EventStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.getEventById(10L)
        );
        verify(eventMapper, never()).toResponse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void normalEventListExcludesCancelledAndDeletedEvents() {
        when(eventRepository
                .findByEventDateGreaterThanEqualAndStatusNotInOrderByEventDateAsc(
                        org.mockito.ArgumentMatchers.any(LocalDate.class),
                        eq(List.of(EventStatus.CANCELLED, EventStatus.DELETED))
                ))
                .thenReturn(List.of());

        eventService.getAllEvents();

        verify(eventRepository)
                .findByEventDateGreaterThanEqualAndStatusNotInOrderByEventDateAsc(
                        org.mockito.ArgumentMatchers.any(LocalDate.class),
                        eq(List.of(EventStatus.CANCELLED, EventStatus.DELETED))
                );
    }

    @Test
    void organizationEventListExcludesDeletedEvents() {
        when(organizationRepository.existsById(2L)).thenReturn(true);
        when(eventRepository.findByOrganizationIdAndStatusNot(
                2L,
                EventStatus.DELETED
        )).thenReturn(List.of());

        eventService.getEventsByOrganization(2L);

        verify(eventRepository).findByOrganizationIdAndStatusNot(
                2L,
                EventStatus.DELETED
        );
    }

    private EventEntity eventLedBy(String leaderUsername) {
        UserEntity leader = UserEntity.builder()
                .username(leaderUsername)
                .build();
        OrganizationEntity organization = OrganizationEntity.builder()
                .leader(leader)
                .build();
        return EventEntity.builder()
                .id(10L)
                .status(EventStatus.UPCOMING)
                .organization(organization)
                .build();
    }
}
