package com.collegeeventhub.admin.service;

import com.collegeeventhub.club.dto.ClubResponse;
import com.collegeeventhub.club.entity.ClubEntity;
import com.collegeeventhub.club.repository.ClubRepository;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class AdminClubModerationIntegrationTest {

    @Autowired
    private AdminClubService adminClubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void filtersClubsAndAllowsAdminToDeactivateAndReactivate() {
        UserEntity leader = userRepository.saveAndFlush(user());
        OrganizationEntity organization =
                organizationRepository.saveAndFlush(organization(leader));
        ClubEntity club = clubRepository.saveAndFlush(
                club(organization, leader, true)
        );
        long activeClubCount = clubRepository.findAllByFilters(
                true,
                true,
                PageRequest.of(0, 1)
        ).getTotalElements();
        long inactiveClubCount = clubRepository.findAllByFilters(
                true,
                false,
                PageRequest.of(0, 1)
        ).getTotalElements();
        ClubEntity inactiveClub = clubRepository.saveAndFlush(
                club(organization, leader, false)
        );

        PageResponse<ClubResponse> inactivePage =
                adminClubService.getAllClubs(0, 1, false);
        assertEquals(inactiveClubCount + 1, inactivePage.getTotalElements());
        assertEquals(1, inactivePage.getContent().size());
        assertEquals(false, inactivePage.getContent().get(0).getActive());

        PageResponse<ClubResponse> activePage =
                adminClubService.getAllClubs(0, 1, true);
        assertEquals(activeClubCount, activePage.getTotalElements());
        assertEquals(1, activePage.getContent().size());
        assertEquals(true, activePage.getContent().get(0).getActive());

        adminClubService.deactivateClub(club.getId());
        assertEquals(false, clubRepository.findById(club.getId())
                .orElseThrow().getActive());

        adminClubService.activateClub(inactiveClub.getId());
        assertEquals(true, clubRepository.findById(inactiveClub.getId())
                .orElseThrow().getActive());
    }

    @Test
    void missingClubCannotBeActivatedOrDeactivated() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> adminClubService.activateClub(-1L)
        );
        assertThrows(
                ResourceNotFoundException.class,
                () -> adminClubService.deactivateClub(-1L)
        );
    }

    private UserEntity user() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return UserEntity.builder()
                .username("club-leader-" + suffix)
                .email("club-leader-" + suffix + "@example.com")
                .password("test-password")
                .firstName("Club")
                .lastName("Leader")
                .role(Role.STUDENT)
                .active(true)
                .build();
    }

    private OrganizationEntity organization(UserEntity leader) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return OrganizationEntity.builder()
                .name("Club Org " + suffix)
                .leader(leader)
                .active(true)
                .build();
    }

    private ClubEntity club(
            OrganizationEntity organization,
            UserEntity leader,
            boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return ClubEntity.builder()
                .name("Moderated Club " + suffix)
                .organization(organization)
                .leader(leader)
                .active(active)
                .build();
    }
}
