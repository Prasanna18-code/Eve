package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.event.dto.EventRequest;
import com.collegeeventhub.event.dto.EventResponse;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.mapper.EventMapper;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.service.NotificationService;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final OrganizationRepository organizationRepository;
        private final UserRepository userRepository;
        private final RegistrationRepository registrationRepository;
        private final NotificationService notificationService;
        private final StudentProfileVerificationGuard studentProfileVerificationGuard;

    @Override
    public EventResponse createEvent(
            Long organizationId,
            String username,
            EventRequest request) {

        OrganizationEntity organization =
                organizationRepository.findById(organizationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Organization not found with id: "
                                                + organizationId));

        verifyLeader(organization, username);

        EventEntity event = eventMapper.toEntity(request);
        event.setOrganization(organization);

        EventEntity savedEvent = eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    public List<EventResponse> getAllEvents() {

        studentProfileVerificationGuard.checkAuthenticatedUser();

        return eventRepository
                .findByEventDateGreaterThanEqualAndStatusNotInOrderByEventDateAsc(
                        LocalDate.now(),
                        List.of(EventStatus.CANCELLED, EventStatus.DELETED)
                )
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    public List<EventResponse> getCancelledEvents() {

        studentProfileVerificationGuard.checkAuthenticatedUser();

        return eventRepository
                .findByEventDateGreaterThanEqualAndStatusOrderByEventDateAsc(
                        LocalDate.now(),
                        EventStatus.CANCELLED
                )
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    public EventResponse getEventById(Long id) {

        studentProfileVerificationGuard.checkAuthenticatedUser();

        EventEntity event =
                eventRepository.findByIdAndStatusNot(id, EventStatus.DELETED)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + id));

        return eventMapper.toResponse(event);
    }

    @Override
    public List<EventResponse> getEventsByOrganization(
            Long organizationId) {

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException(
                    "Organization not found with id: " + organizationId);
        }

        return eventRepository.findByOrganizationIdAndStatusNot(
                        organizationId,
                        EventStatus.DELETED
                )
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    public EventResponse updateEvent(
            Long eventId,
            String username,
            EventRequest request) {

        EventEntity event =
                eventRepository.findByIdAndStatusNot(eventId, EventStatus.DELETED)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + eventId));

        verifyLeader(event.getOrganization(), username);

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setVenue(request.getVenue());
        event.setCapacity(request.getCapacity());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setType(request.getType());
        if (request.wasEligibleBranchesProvided()
                && request.getEligibleBranches() != null) {
            event.setEligibleBranches(new HashSet<>(request.getEligibleBranches()));
        }
        if (request.wasEligibleAcademicYearsProvided()
                && request.getEligibleAcademicYears() != null) {
            event.setEligibleAcademicYears(
                    new HashSet<>(request.getEligibleAcademicYears()));
        }
        if (request.wasEligibleSectionsProvided()
                && request.getEligibleSections() != null) {
            event.setEligibleSections(new HashSet<>(request.getEligibleSections()));
        }

        EventEntity updatedEvent = eventRepository.save(event);

        List<RegistrationEntity> registrations =
                registrationRepository.findByEventId(eventId);

        for (RegistrationEntity registration : registrations) {
            notificationService.createNotification(
                    registration.getUser().getId(),
                    event.getId(),
                    "Event Updated",
                    "The event \"" + event.getTitle() + "\" has been updated.",
                    NotificationType.EVENT_UPDATED
            );
        }

        return eventMapper.toResponse(updatedEvent);
    }

    @Override
    public void deleteEvent(
            Long eventId,
            String username) {

        EventEntity event =
                eventRepository.findByIdAndStatusNot(eventId, EventStatus.DELETED)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + eventId));

        verifyLeader(event.getOrganization(), username);

        event.setStatus(EventStatus.DELETED);
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void cancelEvent(
            Long eventId,
            String username) {

        EventEntity event =
                eventRepository.findByIdAndStatusNot(eventId, EventStatus.DELETED)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + eventId));

        verifyLeader(event.getOrganization(), username);

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        List<RegistrationEntity> registrations =
                registrationRepository.findByEventId(eventId);

        for (RegistrationEntity registration : registrations) {
            notificationService.createNotification(
                    registration.getUser().getId(),
                    event.getId(),
                    "Event Cancelled",
                    "The event \"" + event.getTitle() + "\" has been cancelled.",
                    NotificationType.EVENT_CANCELLED
            );
        }
    }

    private void verifyLeader(
            OrganizationEntity organization,
            String username) {

        if (!organization.getLeader()
                .getUsername()
                .equals(username)) {

            throw new AccessDeniedException(
                    "You are not the leader of this organization");
        }
    }
}
