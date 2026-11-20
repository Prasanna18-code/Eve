package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminEventResponse;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final EventService eventService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminEventResponse> getAllEvents() {

        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelEvent(Long eventId) {
        eventService.cancelEventByAdmin(eventId);
    }

    private AdminEventResponse toResponse(EventEntity event) {

        return AdminEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .venue(event.getVenue())
                .capacity(event.getCapacity())
                .registrationDeadline(event.getRegistrationDeadline())
                .type(event.getType())
                .status(event.getStatus())
                .organizationId(event.getOrganization().getId())
                .organizationName(event.getOrganization().getName())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
