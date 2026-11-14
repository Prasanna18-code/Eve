package com.collegeeventhub.registration.service;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.service.NotificationService;
import com.collegeeventhub.registration.dto.RegistrationResponse;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.mapper.RegistrationMapper;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final RegistrationMapper registrationMapper;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
        private final NotificationService notificationService;
    private final StudentProfileVerificationGuard studentProfileVerificationGuard;

    @Override
        @Transactional
    public RegistrationResponse registerForEvent(
            Long eventId,
            String username
    ) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        studentProfileVerificationGuard.checkUser(user);

        EventEntity event = eventRepository.findByIdAndStatusNot(
                        eventId,
                        EventStatus.DELETED
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));

        if (registrationRepository.existsByUserIdAndEventId(
                user.getId(),
                eventId
        )) {
            throw new IllegalStateException(
                    "You are already registered for this event"
            );
        }

        long registeredCount =
                registrationRepository.countByEventId(eventId);

        if (registeredCount >= event.getCapacity()) {
            throw new IllegalStateException("Event is full");
        }

        RegistrationEntity registration =
                RegistrationEntity.builder()
                        .user(user)
                        .event(event)
                        .build();

        RegistrationEntity saved =
                registrationRepository.save(registration);

        notificationService.createNotification(
                user.getId(),
                "Registration Successful",
                "You have successfully registered for " + event.getTitle(),
                NotificationType.REGISTRATION_SUCCESSFUL
        );

        return registrationMapper.toResponse(saved);
    }

    @Override
    public void cancelRegistration(
            Long eventId,
            String username
    ) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        studentProfileVerificationGuard.checkUser(user);

        RegistrationEntity registration =
                registrationRepository.findByUserIdAndEventId(
                                user.getId(),
                                eventId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Registration not found"
                                ));

        registrationRepository.delete(registration);
    }

    @Override
    public List<RegistrationResponse> getMyRegistrations(
            String username
    ) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        studentProfileVerificationGuard.checkUser(user);

        return registrationRepository.findByUserId(user.getId())
                .stream()
                .map(registrationMapper::toResponse)
                .toList();
    }

    @Override
    public List<RegistrationResponse> getEventRegistrations(
            Long eventId,
            String username
    ) {

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));

        if (!event.getOrganization()
                .getLeader()
                .getUsername()
                .equals(username)) {

            throw new AccessDeniedException(
                    "You are not allowed to view event registrations"
            );
        }

        return registrationRepository.findByEventId(eventId)
                .stream()
                .map(registrationMapper::toResponse)
                .toList();
    }
}
