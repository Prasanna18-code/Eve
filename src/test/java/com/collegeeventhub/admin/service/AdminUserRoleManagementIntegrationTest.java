package com.collegeeventhub.admin.service;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@Rollback
class AdminUserRoleManagementIntegrationTest {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void promotesAndDemotesUserWhilePreservingExistingActiveAdmins() {
        int initialActiveAdmins =
                userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN).size();
        UserEntity student = userRepository.saveAndFlush(user(Role.STUDENT));
        assertEquals(
                Role.ADMIN,
                adminUserService.updateUserRole(
                        student.getId(),
                        Role.ADMIN,
                        999L
                ).getRole()
        );
        assertEquals(
                initialActiveAdmins + 1,
                userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN).size()
        );

        assertEquals(
                Role.FACULTY,
                adminUserService.updateUserRole(
                        student.getId(),
                        Role.FACULTY,
                        999L
                ).getRole()
        );
        assertEquals(
                initialActiveAdmins,
                userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN).size()
        );
    }

    private UserEntity user(Role role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return UserEntity.builder()
                .username("role-" + suffix)
                .email("role-" + suffix + "@example.com")
                .password("test-password")
                .firstName("Role")
                .lastName("Test")
                .role(role)
                .active(true)
                .build();
    }
}
