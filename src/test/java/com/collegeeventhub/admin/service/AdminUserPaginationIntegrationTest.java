package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminUserPageResponse;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class AdminUserPaginationIntegrationTest {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void filtersBeforePagingAndReturnsStablePageMetadata() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long existingMatchingUsers = userRepository.findAllByFilters(
                Role.STUDENT,
                false,
                PageRequest.of(0, 1)
        ).getTotalElements();
        List<UserEntity> matchingUsers = userRepository.saveAll(List.of(
                user("inactive-student-one", suffix, Role.STUDENT, false),
                user("inactive-student-two", suffix, Role.STUDENT, false),
                user("inactive-student-three", suffix, Role.STUDENT, false)
        ));
        userRepository.saveAll(List.of(
                user("active-student", suffix, Role.STUDENT, true),
                user("inactive-faculty", suffix, Role.FACULTY, false)
        ));

        AdminUserPageResponse page = adminUserService.getAllUsers(
                1,
                2,
                Role.STUDENT,
                false
        );

        long expectedTotalElements = existingMatchingUsers + matchingUsers.size();
        assertEquals(1, page.getPage());
        assertEquals(2, page.getSize());
        assertEquals(expectedTotalElements, page.getTotalElements());
        int expectedTotalPages = (int) Math.ceil(expectedTotalElements / 2.0);
        assertEquals(expectedTotalPages, page.getTotalPages());
        assertEquals(page.getPage() == expectedTotalPages - 1, page.isLast());
        assertEquals((int) Math.min(2, expectedTotalElements - 2), page.getContent().size());
        assertTrue(page.getContent().stream().allMatch(user ->
                !user.getActive() && user.getRole() == Role.STUDENT));
        assertFalse(page.getContent().isEmpty());
    }

    private UserEntity user(
            String username,
            String suffix,
            Role role,
            boolean active) {
        return UserEntity.builder()
                .username(username + "_" + suffix)
                .email(username.replace('-', '.') + "_" + suffix + "@example.com")
                .password("test-password")
                .firstName("Test")
                .lastName(username)
                .role(role)
                .active(active)
                .build();
    }
}
