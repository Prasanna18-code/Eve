package com.collegeeventhub.event.service;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventAudienceTargetingTest {

    private static final Long EVENT_ID = 10L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventMapper mapper = new EventMapper();

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

    private EventEntity event;

    @BeforeEach
    void setUp() {
        UserEntity leader = UserEntity.builder()
                .username("leader")
                .build();
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(2L)
                .name("Test organization")
                .leader(leader)
                .build();
        event = EventEntity.builder()
                .id(EVENT_ID)
                .status(EventStatus.UPCOMING)
                .organization(organization)
                .eligibleBranches(new HashSet<>(Set.of("CSE", "ECE")))
                .eligibleAcademicYears(new HashSet<>(Set.of(2, 3)))
                .eligibleSections(new HashSet<>(Set.of("A")))
                .build();
    }

    @Test
    void mapperDefaultsMissingAndNullTargetingAndCopiesRequestCollections()
            throws Exception {
        EventEntity omitted = mapper.toEntity(readRequest("{}"));
        EventEntity explicitNulls = mapper.toEntity(readRequest(
                "{\"eligibleBranches\":null,"
                        + "\"eligibleAcademicYears\":null,"
                        + "\"eligibleSections\":null}"
        ));

        assertTrue(omitted.getEligibleBranches().isEmpty());
        assertTrue(omitted.getEligibleAcademicYears().isEmpty());
        assertTrue(omitted.getEligibleSections().isEmpty());
        assertTrue(explicitNulls.getEligibleBranches().isEmpty());
        assertTrue(explicitNulls.getEligibleAcademicYears().isEmpty());
        assertTrue(explicitNulls.getEligibleSections().isEmpty());

        Set<String> branches = new HashSet<>(Set.of("CSE"));
        EventRequest request = new EventRequest();
        request.setEligibleBranches(branches);
        branches.add("ECE");

        EventEntity copied = mapper.toEntity(request);
        assertEquals(Set.of("CSE"), copied.getEligibleBranches());
        assertNotSame(request.getEligibleBranches(), copied.getEligibleBranches());
    }

    @Test
    void mapperReturnsDefensiveCopiesOfEntityTargeting() {
        EventResponse response = mapper.toResponse(event);

        assertEquals(event.getEligibleBranches(), response.getEligibleBranches());
        assertEquals(event.getEligibleAcademicYears(), response.getEligibleAcademicYears());
        assertEquals(event.getEligibleSections(), response.getEligibleSections());
        assertNotSame(event.getEligibleBranches(), response.getEligibleBranches());
        assertNotSame(event.getEligibleAcademicYears(), response.getEligibleAcademicYears());
        assertNotSame(event.getEligibleSections(), response.getEligibleSections());

        response.getEligibleBranches().clear();
        response.getEligibleAcademicYears().clear();
        response.getEligibleSections().clear();

        assertEquals(Set.of("CSE", "ECE"), event.getEligibleBranches());
        assertEquals(Set.of(2, 3), event.getEligibleAcademicYears());
        assertEquals(Set.of("A"), event.getEligibleSections());
    }

    @Test
    void updatePreservesTargetingWhenFieldsAreOmittedOrExplicitlyNull()
            throws Exception {
        EventRequest omitted = readRequest("{}");
        EventRequest explicitNulls = readRequest(
                "{\"eligibleBranches\":null,"
                        + "\"eligibleAcademicYears\":null,"
                        + "\"eligibleSections\":null}"
        );

        assertFalse(omitted.wasEligibleBranchesProvided());
        assertFalse(omitted.wasEligibleAcademicYearsProvided());
        assertFalse(omitted.wasEligibleSectionsProvided());
        assertTrue(explicitNulls.wasEligibleBranchesProvided());
        assertTrue(explicitNulls.wasEligibleAcademicYearsProvided());
        assertTrue(explicitNulls.wasEligibleSectionsProvided());

        prepareUpdateStubs();
        eventService.updateEvent(EVENT_ID, "leader", omitted);
        eventService.updateEvent(EVENT_ID, "leader", explicitNulls);

        assertEquals(Set.of("CSE", "ECE"), event.getEligibleBranches());
        assertEquals(Set.of(2, 3), event.getEligibleAcademicYears());
        assertEquals(Set.of("A"), event.getEligibleSections());
    }

    @Test
    void updateClearsExplicitEmptyCollectionsAndReplacesNonEmptyCollections()
            throws Exception {
        EventRequest request = readRequest(
                "{\"eligibleBranches\":[],"
                        + "\"eligibleAcademicYears\":[1,4],"
                        + "\"eligibleSections\":[\"B\",\"C\"]}"
        );

        prepareUpdateStubs();
        eventService.updateEvent(EVENT_ID, "leader", request);

        assertTrue(event.getEligibleBranches().isEmpty());
        assertEquals(Set.of(1, 4), event.getEligibleAcademicYears());
        assertEquals(Set.of("B", "C"), event.getEligibleSections());
    }

    private EventRequest readRequest(String json) throws Exception {
        return objectMapper.readValue(json, EventRequest.class);
    }

    private void prepareUpdateStubs() {
        when(eventRepository.findByIdAndStatusNot(EVENT_ID, EventStatus.DELETED))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(any(EventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationRepository.findByEventId(EVENT_ID))
                .thenReturn(List.of());
    }
}
