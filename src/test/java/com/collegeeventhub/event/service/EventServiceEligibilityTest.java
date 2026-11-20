package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceEligibilityTest {

    private static final Long ORGANIZATION_ID = 4L;
    private static final Long ELIGIBLE_EVENT_ID = 10L;
    private static final Long INELIGIBLE_EVENT_ID = 11L;

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

    private EventServiceImpl eventService;

    private UserEntity student;
    private EventEntity eligibleEvent;
    private EventEntity ineligibleEvent;

    @BeforeEach
    void setUp() {
        student = UserEntity.builder()
                .username("verified-student")
                .role(com.collegeeventhub.common.enums.Role.STUDENT)
                .profileVerified(true)
                .branch("CSE")
                .academicYear(3)
                .section("A")
                .build();
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(ORGANIZATION_ID)
                .build();
        eligibleEvent = event(
                ELIGIBLE_EVENT_ID,
                organization,
                Set.of("CSE", "ECE"),
                Set.of(3),
                Set.of("A", "B")
        );
        ineligibleEvent = event(
                INELIGIBLE_EVENT_ID,
                organization,
                Set.of("ME"),
                Set.of(3),
                Set.of("A")
        );

        eventService = new EventServiceImpl(
                eventRepository,
                eventMapper,
                organizationRepository,
                userRepository,
                registrationRepository,
                notificationService,
                studentProfileVerificationGuard,
                eventEligibilityService
        );

        when(studentProfileVerificationGuard.checkAuthenticatedUser())
                .thenReturn(student);
        org.mockito.Mockito.lenient().when(eventMapper.toResponse(eligibleEvent))
                .thenReturn(EventResponse.builder().id(ELIGIBLE_EVENT_ID).build());
        org.mockito.Mockito.lenient().when(eventMapper.toResponse(ineligibleEvent))
                .thenReturn(EventResponse.builder().id(INELIGIBLE_EVENT_ID).build());
        org.mockito.Mockito.lenient().doThrow(new AccessDeniedException("You are not eligible for this event"))
                .when(eventEligibilityService)
                .checkEligible(student, ineligibleEvent);
    }

    @Test
    void mainEventListingRequestsAudienceFilteredPageForStudent() {
        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Order.asc("eventDate"),
                        Sort.Order.asc("startTime"),
                        Sort.Order.asc("id")
                )
        );
        when(eventRepository.findAllByListingFilters(
                eq(true),
                any(LocalDate.class),
                eq(false),
                eq(EventStatus.UPCOMING),
                eq(true),
                eq(true),
                eq(EventStatus.CANCELLED),
                eq(EventStatus.DELETED),
                eq(false),
                eq(EventType.ONLINE),
                eq(false),
                eq(-1L),
                eq(true),
                eq(true),
                eq("CSE"),
                eq(true),
                eq(3),
                eq(true),
                eq("A"),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(eligibleEvent), pageable, 1));

        var result = eventService.getAllEvents(0, 10, null, null);

        assertEquals(List.of(ELIGIBLE_EVENT_ID), result.getContent().stream()
                .map(EventResponse::getId)
                .toList());
    }

    @Test
    void cancelledEventListingRequestsCancelledAudienceFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("eventDate", "startTime", "id"));
        when(eventRepository.findAllByListingFilters(
                eq(true),
                any(LocalDate.class),
                eq(true),
                eq(EventStatus.CANCELLED),
                eq(false),
                eq(true),
                eq(EventStatus.CANCELLED),
                eq(EventStatus.DELETED),
                eq(false),
                eq(EventType.ONLINE),
                eq(false),
                eq(-1L),
                eq(true),
                eq(true),
                eq("CSE"),
                eq(true),
                eq(3),
                eq(true),
                eq("A"),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(eligibleEvent), pageable, 1));

        var result = eventService.getCancelledEvents(0, 10, null, null);

        assertEquals(List.of(ELIGIBLE_EVENT_ID), result.getContent().stream()
                .map(EventResponse::getId)
                .toList());
    }

    @Test
    void organizationEventListingRequestsAudienceFilteredPageAndRequiresVerifiedStudent() {
        when(organizationRepository.existsById(ORGANIZATION_ID)).thenReturn(true);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("eventDate", "startTime", "id"));
        when(eventRepository.findAllByListingFilters(
                eq(false),
                eq(LocalDate.of(1970, 1, 1)),
                eq(false),
                eq(EventStatus.UPCOMING),
                eq(false),
                eq(true),
                eq(EventStatus.CANCELLED),
                eq(EventStatus.DELETED),
                eq(false),
                eq(EventType.ONLINE),
                eq(true),
                eq(ORGANIZATION_ID.longValue()),
                eq(true),
                eq(true),
                eq("CSE"),
                eq(true),
                eq(3),
                eq(true),
                eq("A"),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(eligibleEvent), pageable, 1));

        var result = eventService.getEventsByOrganization(
                ORGANIZATION_ID,
                0,
                10,
                null
        );

        assertEquals(List.of(ELIGIBLE_EVENT_ID), result.getContent().stream()
                .map(EventResponse::getId)
                .toList());
        verify(studentProfileVerificationGuard).checkAuthenticatedUser();
    }

    @Test
    void rejectsIneligibleStudentOnDirectEventLookup() {
        when(eventRepository.findByIdAndStatusNot(
                INELIGIBLE_EVENT_ID,
                EventStatus.DELETED
        )).thenReturn(Optional.of(ineligibleEvent));

        assertThrows(
                AccessDeniedException.class,
                () -> eventService.getEventById(INELIGIBLE_EVENT_ID)
        );
        verify(eventMapper, never()).toResponse(ineligibleEvent);
    }

    @Test
    void unverifiedStudentIsRejectedBeforeOrganizationEventListing() {
        when(studentProfileVerificationGuard.checkAuthenticatedUser())
                .thenThrow(new AccessDeniedException("Profile verification required"));

        assertThrows(
                AccessDeniedException.class,
                () -> eventService.getEventsByOrganization(
                        ORGANIZATION_ID,
                        0,
                        10,
                        null
                )
        );
        verify(organizationRepository, never()).existsById(ORGANIZATION_ID);
    }

    private EventEntity event(
            Long id,
            OrganizationEntity organization,
            Set<String> branches,
            Set<Integer> years,
            Set<String> sections) {
        return EventEntity.builder()
                .id(id)
                .status(EventStatus.UPCOMING)
                .organization(organization)
                .eligibleBranches(new HashSet<>(branches))
                .eligibleAcademicYears(new HashSet<>(years))
                .eligibleSections(new HashSet<>(sections))
                .build();
    }
}
