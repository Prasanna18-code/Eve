package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.event.dto.EventRequest;
import com.collegeeventhub.event.dto.EventResponse;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceProfileVerificationTest {

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

    @Mock
    private EventEligibilityService eventEligibilityService;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void eventReadOperationsCheckStudentProfileVerification() {
        when(studentProfileVerificationGuard.checkAuthenticatedUser())
                .thenReturn(UserEntity.builder().role(Role.CLUB_ADMIN).build());
        when(eventRepository
                .findByEventDateGreaterThanEqualAndStatusNotInOrderByEventDateAsc(
                        org.mockito.ArgumentMatchers.any(LocalDate.class),
                        org.mockito.ArgumentMatchers.any()
                ))
                .thenReturn(List.of());
        when(eventRepository
                .findByEventDateGreaterThanEqualAndStatusOrderByEventDateAsc(
                        org.mockito.ArgumentMatchers.any(LocalDate.class),
                        org.mockito.ArgumentMatchers.any()
                ))
                .thenReturn(List.of());

        eventService.getAllEvents();
        eventService.getCancelledEvents();
        verify(studentProfileVerificationGuard, org.mockito.Mockito.times(2))
                .checkAuthenticatedUser();

        EventEntity event = EventEntity.builder().id(10L).build();
        when(eventRepository.findByIdAndStatusNot(10L, EventStatus.DELETED))
                .thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(EventResponse.builder().id(10L).build());

        assertEquals(10L, eventService.getEventById(10L).getId());
        verify(studentProfileVerificationGuard, org.mockito.Mockito.times(3))
                .checkAuthenticatedUser();
    }

    @Test
    void authorizedClubAdminCanCreateAndCancelEventWithoutStudentVerificationCheck() {
        UserEntity host = UserEntity.builder()
                .username("club-host")
                .role(Role.CLUB_ADMIN)
                .profileVerified(false)
                .build();
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(2L)
                .leader(host)
                .build();
        EventEntity event = EventEntity.builder()
                .id(10L)
                .title("Hosted event")
                .organization(organization)
                .build();

        when(organizationRepository.findById(2L))
                .thenReturn(Optional.of(organization));
        when(eventMapper.toEntity(org.mockito.ArgumentMatchers.any(EventRequest.class)))
                .thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event))
                .thenReturn(EventResponse.builder().id(10L).build());
        when(eventRepository.findByIdAndStatusNot(10L, EventStatus.DELETED))
                .thenReturn(Optional.of(event));
        when(registrationRepository.findByEventId(10L)).thenReturn(List.of());

        EventResponse created = eventService.createEvent(
                2L,
                "club-host",
                new EventRequest()
        );
        eventService.cancelEvent(10L, "club-host");

        assertEquals(10L, created.getId());
        verify(studentProfileVerificationGuard, never()).checkAuthenticatedUser();
        verify(eventRepository, org.mockito.Mockito.times(2)).save(event);
        assertEquals(com.collegeeventhub.common.enums.EventStatus.CANCELLED, event.getStatus());
    }
}
