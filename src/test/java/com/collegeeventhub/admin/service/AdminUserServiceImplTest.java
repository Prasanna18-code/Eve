package com.collegeeventhub.admin.service;

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

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(userRepository);
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
