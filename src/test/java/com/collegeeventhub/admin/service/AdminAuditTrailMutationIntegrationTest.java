package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.audit.dto.AdminAuditLogResponse;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.club.entity.ClubEntity;
import com.collegeeventhub.club.repository.ClubRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import com.collegeeventhub.common.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class AdminAuditTrailMutationIntegrationTest {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AdminOrganizationService adminOrganizationService;

    @Autowired
    private AdminClubService adminClubService;

    @Autowired
    private AdminAuditLogService adminAuditLogService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void successfulAdminMutationsAreAuditedWithActorAndStateChanges() {
        UserEntity admin = user(Role.ADMIN, true);
        UserEntity inactiveUser = user(Role.STUDENT, false);
        userRepository.saveAndFlush(admin);
        userRepository.saveAndFlush(inactiveUser);

        OrganizationEntity organization = organizationRepository.saveAndFlush(
                OrganizationEntity.builder()
                        .name("Audit Organization " + UUID.randomUUID())
                        .leader(admin)
                        .active(false)
                        .build()
        );
        ClubEntity club = clubRepository.saveAndFlush(
                ClubEntity.builder()
                        .name("Audit Club " + UUID.randomUUID())
                        .organization(organization)
                        .leader(admin)
                        .active(true)
                        .build()
        );

        adminUserService.activateUser(inactiveUser.getId(), admin.getId());
        adminOrganizationService.activateOrganization(
                organization.getId(),
                admin.getId()
        );
        adminClubService.deactivateClub(club.getId(), admin.getId());

        PageResponse<AdminAuditLogResponse> logs =
                adminAuditLogService.getAuditLogs(
                        0,
                        10,
                        admin.getId(),
                        null,
                        null
                );

        Set<AdminAuditAction> actions = logs.getContent().stream()
                .map(AdminAuditLogResponse::getAction)
                .collect(Collectors.toSet());
        assertEquals(3, logs.getTotalElements());
        assertEquals(Set.of(
                AdminAuditAction.USER_ACTIVATED,
                AdminAuditAction.ORGANIZATION_ACTIVATED,
                AdminAuditAction.CLUB_DEACTIVATED
        ), actions);
        assertTrue(logs.getContent().stream().allMatch(log ->
                admin.getId().equals(log.getAdminUserId())));
        assertTrue(logs.getContent().stream().anyMatch(log ->
                log.getTargetType() == AdminAuditTargetType.USER
                        && log.getTargetId().equals(inactiveUser.getId())
                        && log.getDetails().equals(
                        "active changed from false to true")));
        assertTrue(logs.getContent().stream().anyMatch(log ->
                log.getTargetType() == AdminAuditTargetType.ORGANIZATION
                        && log.getTargetId().equals(organization.getId())
                        && log.getDetails().equals(
                        "active changed from false to true")));
        assertTrue(logs.getContent().stream().anyMatch(log ->
                log.getTargetType() == AdminAuditTargetType.CLUB
                        && log.getTargetId().equals(club.getId())
                        && log.getDetails().equals(
                        "active changed from true to false")));
    }

    private UserEntity user(Role role, boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return UserEntity.builder()
                .username("audit-" + suffix)
                .email("audit-" + suffix + "@example.com")
                .password("test-password")
                .firstName("Audit")
                .lastName("User")
                .role(role)
                .active(active)
                .build();
    }
}
