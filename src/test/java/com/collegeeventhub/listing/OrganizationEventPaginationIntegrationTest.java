package com.collegeeventhub.listing;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class OrganizationEventPaginationIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void organizationActiveFilterIsAppliedBeforePaging() {
        long inactiveOrganizations = organizationRepository.findAllByFilters(
                true,
                false,
                PageRequest.of(0, 1)
        ).getTotalElements();
        UserEntity leader = userRepository.saveAndFlush(user("org"));
        OrganizationEntity inactiveOrganization =
                organizationRepository.saveAndFlush(organization(leader, false));
        organizationRepository.saveAndFlush(organization(leader, true));

        Page<OrganizationEntity> page = organizationRepository.findAllByFilters(
                true,
                false,
                PageRequest.of(0, 1, Sort.by(Sort.Order.asc("id")))
        );

        assertEquals(inactiveOrganizations + 1, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals(false, page.getContent().get(0).getActive());
        assertTrue(page.getNumberOfElements() <= page.getSize());
        assertTrue(organizationRepository.findById(inactiveOrganization.getId()).isPresent());
    }

    @Test
    void eventAudienceAndFiltersAreAppliedBeforePaging() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity leader = userRepository.saveAndFlush(user("event" + suffix));
        OrganizationEntity organization =
                organizationRepository.saveAndFlush(organization(leader, true));

        EventEntity cseEvent = event(organization, "CSE" + suffix);
        cseEvent.setEligibleBranches(Set.of("CSE"));
        eventRepository.saveAndFlush(cseEvent);

        EventEntity eceEvent = event(organization, "ECE" + suffix);
        eceEvent.setEligibleBranches(Set.of("ECE"));
        eventRepository.saveAndFlush(eceEvent);

        EventEntity collegeWideEvent = event(organization, "ALL" + suffix);
        eventRepository.saveAndFlush(collegeWideEvent);

        Page<EventEntity> firstPage = eventRepository.findAllByListingFilters(
                true,
                LocalDate.now(),
                false,
                EventStatus.UPCOMING,
                true,
                true,
                EventStatus.CANCELLED,
                EventStatus.DELETED,
                true,
                EventType.OFFLINE,
                true,
                organization.getId(),
                true,
                true,
                "CSE",
                true,
                3,
                true,
                "A",
                PageRequest.of(
                        0,
                        1,
                        Sort.by(
                                Sort.Order.asc("eventDate"),
                                Sort.Order.asc("startTime"),
                                Sort.Order.asc("id")
                        )
                )
        );

        Page<EventEntity> secondPage = eventRepository.findAllByListingFilters(
                true,
                LocalDate.now(),
                false,
                EventStatus.UPCOMING,
                true,
                true,
                EventStatus.CANCELLED,
                EventStatus.DELETED,
                true,
                EventType.OFFLINE,
                true,
                organization.getId(),
                true,
                true,
                "CSE",
                true,
                3,
                true,
                "A",
                PageRequest.of(
                        1,
                        1,
                        Sort.by(
                                Sort.Order.asc("eventDate"),
                                Sort.Order.asc("startTime"),
                                Sort.Order.asc("id")
                        )
                )
        );
        Page<EventEntity> adminPage = eventRepository.findAllByListingFilters(
                false,
                LocalDate.of(1970, 1, 1),
                true,
                EventStatus.UPCOMING,
                false,
                false,
                EventStatus.CANCELLED,
                EventStatus.DELETED,
                true,
                EventType.OFFLINE,
                true,
                organization.getId(),
                false,
                false,
                "",
                false,
                0,
                false,
                "",
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("id")))
        );

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(1, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
        assertTrue(Set.of(cseEvent.getId(), collegeWideEvent.getId())
                .contains(firstPage.getContent().get(0).getId()));
        assertTrue(Set.of(cseEvent.getId(), collegeWideEvent.getId())
                .contains(secondPage.getContent().get(0).getId()));
        assertTrue(!firstPage.getContent().get(0).getId().equals(eceEvent.getId()));
        assertTrue(!secondPage.getContent().get(0).getId().equals(eceEvent.getId()));
        assertEquals(3, adminPage.getTotalElements());
    }

    private UserEntity user(String suffix) {
        return UserEntity.builder()
                .username("listing-" + suffix)
                .email("listing-" + suffix + "@example.com")
                .password("test-password")
                .firstName("Listing")
                .lastName("Test")
                .role(Role.STUDENT)
                .active(true)
                .profileVerified(true)
                .branch("CSE")
                .academicYear(3)
                .section("A")
                .build();
    }

    private OrganizationEntity organization(UserEntity leader, boolean active) {
        return OrganizationEntity.builder()
                .name("Listing Org " + UUID.randomUUID())
                .leader(leader)
                .active(active)
                .build();
    }

    private EventEntity event(OrganizationEntity organization, String title) {
        return EventEntity.builder()
                .title(title)
                .description("Pagination integration test")
                .eventDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .venue("Test venue")
                .capacity(20)
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .type(EventType.OFFLINE)
                .status(EventStatus.UPCOMING)
                .organization(organization)
                .build();
    }
}
