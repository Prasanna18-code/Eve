package com.collegeeventhub.admin.service;

import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
                IllegalStateException.class,
                () -> adminUserService.deactivateUser(7L, "platform-admin")
        );
        verify(userRepository, never()).save(admin);
    }

    @Test
    void deactivateUserDisablesAnotherAccount() {
        UserEntity student = UserEntity.builder()
                .id(8L)
                .username("student")
                .active(true)
                .build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(student));

        adminUserService.deactivateUser(8L, "platform-admin");

        assertFalse(student.getActive());
        verify(userRepository).save(student);
    }
}
