package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.dto.AdminUserPageResponse;
import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.BadRequestException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminAuditLogService adminAuditLogService;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(
                userRepository,
                adminAuditLogService
        );
    }

    @Test
    void deactivateUserRejectsCurrentAdmin() {
        UserEntity admin = UserEntity.builder()
                .id(7L)
                .username("platform-admin")
                .active(true)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        assertThrows(
                BadRequestException.class,
                () -> adminUserService.deactivateUser(7L, 7L)
        );
        verify(userRepository, never()).save(admin);
    }

    @Test
    void deactivateUserPreservesNotFoundBehavior() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminUserService.deactivateUser(404L, 7L)
        );
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deactivateUserAllowsDifferentIdWithSameUsernameAsAdmin() {
        UserEntity student = UserEntity.builder()
                .id(8L)
                .username("platform-admin")
                .active(true)
                .build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(student));

        adminUserService.deactivateUser(8L, 7L);

        assertFalse(student.getActive());
        verify(userRepository).save(student);
        verify(adminAuditLogService).record(
                7L,
                AdminAuditAction.USER_DEACTIVATED,
                AdminAuditTargetType.USER,
                8L,
                "active changed from true to false"
        );
    }

    @Test
    void deactivateUserCannotRemoveLastActiveAdmin() {
        UserEntity admin = UserEntity.builder()
                .id(8L)
                .role(Role.ADMIN)
                .active(true)
                .build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(admin));
        when(userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN))
                .thenReturn(List.of(admin));

        assertThrows(
                BadRequestException.class,
                () -> adminUserService.deactivateUser(8L, 7L)
        );
        verify(userRepository, never()).save(admin);
    }

    @Test
    void updateUserRoleUpdatesAndReturnsUser() {
        UserEntity student = UserEntity.builder()
                .id(8L)
                .username("student")
                .email("student@example.com")
                .firstName("Test")
                .lastName("Student")
                .role(Role.STUDENT)
                .active(true)
                .build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        AdminUserResponse result =
                adminUserService.updateUserRole(8L, Role.FACULTY, 7L);

        assertEquals(Role.FACULTY, student.getRole());
        assertEquals(Role.FACULTY, result.getRole());
        verify(userRepository).save(student);
        verify(adminAuditLogService).record(
                7L,
                AdminAuditAction.USER_ROLE_CHANGED,
                AdminAuditTargetType.USER,
                8L,
                "role changed from STUDENT to FACULTY"
        );
    }

    @Test
    void updateUserRoleRejectsSelfDemotion() {
        UserEntity admin = UserEntity.builder()
                .id(7L)
                .role(Role.ADMIN)
                .active(true)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        assertThrows(
                BadRequestException.class,
                () -> adminUserService.updateUserRole(7L, Role.FACULTY, 7L)
        );
        verify(userRepository, never()).save(admin);
        verify(userRepository, never())
                .findActiveUsersByRoleForUpdate(Role.ADMIN);
    }

    @Test
    void updateUserRoleCannotDemoteLastActiveAdmin() {
        UserEntity admin = UserEntity.builder()
                .id(7L)
                .role(Role.ADMIN)
                .active(true)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN))
                .thenReturn(List.of(admin));

        assertThrows(
                BadRequestException.class,
                () -> adminUserService.updateUserRole(7L, Role.FACULTY, 8L)
        );
        verify(userRepository, never()).save(admin);
    }

    @Test
    void updateUserRoleCanDemoteAnAdminWhenAnotherActiveAdminRemains() {
        UserEntity admin = UserEntity.builder()
                .id(8L)
                .role(Role.ADMIN)
                .active(true)
                .build();
        UserEntity otherAdmin = UserEntity.builder()
                .id(7L)
                .role(Role.ADMIN)
                .active(true)
                .build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(admin));
        when(userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN))
                .thenReturn(List.of(otherAdmin, admin));
        when(userRepository.save(admin)).thenReturn(admin);

        AdminUserResponse result =
                adminUserService.updateUserRole(8L, Role.CLUB_ADMIN, 7L);

        assertEquals(Role.CLUB_ADMIN, result.getRole());
        verify(userRepository).save(admin);
    }

    @Test
    void updateUserRolePreservesNotFoundBehavior() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminUserService.updateUserRole(404L, Role.FACULTY, 7L)
        );
        verify(userRepository, never())
                .findActiveUsersByRoleForUpdate(Role.ADMIN);
    }

    @Test
    void getAllUsersAppliesFiltersAndReturnsRequestedPage() {
        UserEntity inactiveStudent = UserEntity.builder()
                .id(8L)
                .username("inactive-student")
                .email("inactive@example.com")
                .firstName("Inactive")
                .lastName("Student")
                .role(Role.STUDENT)
                .active(false)
                .build();
        PageRequest pageable = PageRequest.of(
                1,
                2,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        when(userRepository.findAllByFilters(Role.STUDENT, false, pageable))
                .thenReturn(new PageImpl<>(
                        List.of(inactiveStudent),
                        pageable,
                        3
                ));

        AdminUserPageResponse result = adminUserService.getAllUsers(
                1,
                2,
                Role.STUDENT,
                false
        );

        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(true, result.isLast());
        assertEquals(1, result.getContent().size());
        AdminUserResponse listedUser = result.getContent().get(0);
        assertEquals(8L, listedUser.getId());
        assertEquals(Role.STUDENT, listedUser.getRole());
        assertEquals(false, listedUser.getActive());
        verify(userRepository).findAllByFilters(Role.STUDENT, false, pageable);
    }
}
