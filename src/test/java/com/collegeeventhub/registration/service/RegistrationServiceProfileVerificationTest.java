package com.collegeeventhub.registration.service;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.event.service.EventEligibilityService;
import com.collegeeventhub.notification.service.NotificationService;
import com.collegeeventhub.registration.dto.RegistrationResponse;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.mapper.RegistrationMapper;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceProfileVerificationTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationMapper registrationMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationService notificationService;

    private RegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(
                registrationRepository,
                registrationMapper,
                userRepository,
                eventRepository,
                notificationService,
                new StudentProfileVerificationGuard(userRepository),
                new EventEligibilityService()
        );
    }

    @Test
    void unverifiedStudentCannotRegisterForEvent() {
        UserEntity student = UserEntity.builder()
                .id(1L)
                .username("unverified")
                .role(Role.STUDENT)
                .profileVerified(false)
                .build();
        when(userRepository.findByUsername("unverified"))
                .thenReturn(Optional.of(student));

        assertThrows(
                AccessDeniedException.class,
                () -> registrationService.registerForEvent(2L, "unverified")
        );

        verify(eventRepository, never()).findByIdAndStatusNot(2L, EventStatus.DELETED);
        verify(registrationRepository, never()).save(any(RegistrationEntity.class));
    }

    @Test
    void verifiedStudentCanRegisterForEvent() {
        UserEntity student = UserEntity.builder()
                .id(1L)
                .username("verified")
                .role(Role.STUDENT)
                .profileVerified(true)
                .build();
        EventEntity event = EventEntity.builder()
                .id(2L)
                .title("Campus event")
                .capacity(10)
                .build();
        RegistrationEntity savedRegistration = RegistrationEntity.builder()
                .id(3L)
                .user(student)
                .event(event)
                .build();
        when(userRepository.findByUsername("verified"))
                .thenReturn(Optional.of(student));
        when(eventRepository.findByIdAndStatusNot(2L, EventStatus.DELETED))
                .thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 2L))
                .thenReturn(false);
        when(registrationRepository.countByEventId(2L)).thenReturn(0L);
        when(registrationRepository.save(any(RegistrationEntity.class)))
                .thenReturn(savedRegistration);
        when(registrationMapper.toResponse(savedRegistration))
                .thenReturn(RegistrationResponse.builder().id(3L).build());

        RegistrationResponse response =
                registrationService.registerForEvent(2L, "verified");

        assertNotNull(response);
        verify(registrationRepository).save(any(RegistrationEntity.class));
    }

    @Test
    void verifiedStudentCannotRegisterForDeletedEvent() {
        UserEntity student = UserEntity.builder()
                .id(1L)
                .username("verified")
                .role(Role.STUDENT)
                .profileVerified(true)
                .build();
        when(userRepository.findByUsername("verified"))
                .thenReturn(Optional.of(student));
        when(eventRepository.findByIdAndStatusNot(2L, EventStatus.DELETED))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> registrationService.registerForEvent(2L, "verified")
        );

        verify(registrationRepository, never()).save(any(RegistrationEntity.class));
    }

    @Test
    void unverifiedStudentCannotCancelRegistrationOrReadOwnRegistrations() {
        UserEntity student = UserEntity.builder()
                .id(1L)
                .username("unverified")
                .role(Role.STUDENT)
                .profileVerified(false)
                .build();
        when(userRepository.findByUsername("unverified"))
                .thenReturn(Optional.of(student));

        assertThrows(
                AccessDeniedException.class,
                () -> registrationService.cancelRegistration(2L, "unverified")
        );
        assertThrows(
                AccessDeniedException.class,
                () -> registrationService.getMyRegistrations("unverified")
        );

        verify(registrationRepository, never()).findByUserId(1L);
        verify(registrationRepository, never()).delete(any(RegistrationEntity.class));
    }
}
