package com.collegeeventhub.common.security;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.GlobalExceptionHandler;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProfileVerificationGuardTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksUnverifiedStudent() {
        StudentProfileVerificationGuard guard =
                new StudentProfileVerificationGuard(userRepository);
        UserEntity user = UserEntity.builder()
                .role(Role.STUDENT)
                .profileVerified(false)
                .build();

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> guard.checkUser(user)
        );

        assertEquals(
                StudentProfileVerificationGuard.VERIFICATION_REQUIRED_MESSAGE,
                exception.getMessage()
        );
    }

    @Test
    void allowsVerifiedStudent() {
        StudentProfileVerificationGuard guard =
                new StudentProfileVerificationGuard(userRepository);
        UserEntity user = UserEntity.builder()
                .role(Role.STUDENT)
                .profileVerified(true)
                .build();

        guard.checkUser(user);
    }

    @Test
    void allowsAdminWithoutDirectoryVerification() {
        StudentProfileVerificationGuard guard =
                new StudentProfileVerificationGuard(userRepository);
        UserEntity user = UserEntity.builder()
                .role(Role.ADMIN)
                .profileVerified(false)
                .build();

        guard.checkUser(user);
    }

    @Test
    void allowsClubAdminWithoutDirectoryVerification() {
        StudentProfileVerificationGuard guard =
                new StudentProfileVerificationGuard(userRepository);
        UserEntity user = UserEntity.builder()
                .role(Role.CLUB_ADMIN)
                .profileVerified(false)
                .build();

        guard.checkUser(user);
    }

    @Test
    void checksTheUserIdentifiedByTheAuthenticatedPrincipal() {
        StudentProfileVerificationGuard guard =
                new StudentProfileVerificationGuard(userRepository);
        UserEntity user = UserEntity.builder()
                .username("verified-student")
                .role(Role.STUDENT)
                .profileVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "verified-student",
                        "not-a-real-credential",
                        List.of()
                )
        );
        when(userRepository.findByUsername("verified-student"))
                .thenReturn(Optional.of(user));

        guard.checkAuthenticatedUser();

        verify(userRepository).findByUsername("verified-student");
    }

    @Test
    void convertsVerificationDenialToForbiddenResponseWithClearMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDeniedException(
                        new AccessDeniedException(
                                StudentProfileVerificationGuard
                                        .VERIFICATION_REQUIRED_MESSAGE
                        )
                );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Assertions.assertEquals(
                StudentProfileVerificationGuard.VERIFICATION_REQUIRED_MESSAGE,
                response.getBody().getMessage()
        );
    }
}
