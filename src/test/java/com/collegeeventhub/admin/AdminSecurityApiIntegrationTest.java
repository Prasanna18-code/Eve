package com.collegeeventhub.admin;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.audit.repository.AdminAuditLogRepository;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.security.JwtService;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AdminSecurityApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    @Test
    void onlyAdminCanDeactivateUsersAndSuccessfulChangesAreAudited()
            throws Exception {
        UserEntity admin = saveUser(Role.ADMIN);
        UserEntity student = saveUser(Role.STUDENT);
        String adminToken = token(admin);
        String studentToken = token(student);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("role", "STUDENT")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].username",
                        hasItem(student.getUsername())));

        mockMvc.perform(put(
                                "/api/v1/admin/users/{userId}/deactivate",
                                student.getId()
                        )
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        assertFalse(userRepository.findById(student.getId())
                .orElseThrow()
                .getActive());
        assertTrue(auditLogRepository
                .findByAdminUserIdAndActionAndTargetTypeAndTargetIdOrderByIdAsc(
                        admin.getId(),
                        AdminAuditAction.USER_DEACTIVATED,
                        AdminAuditTargetType.USER,
                        student.getId()
                )
                .stream()
                .anyMatch(log -> log.getDetails()
                        .equals("active changed from true to false")));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isUnauthorized());
    }

    private UserEntity saveUser(Role role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(UserEntity.builder()
                .username("admin-api-" + role.name().toLowerCase()
                        + "-" + suffix)
                .email("admin-api-" + suffix + "@example.com")
                .password("test-password")
                .firstName("Integration")
                .lastName("User")
                .role(role)
                .active(true)
                .profileVerified(true)
                .build());
    }

    private String token(UserEntity user) {
        return jwtService.generateToken(user.getId(), user.getUsername());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
