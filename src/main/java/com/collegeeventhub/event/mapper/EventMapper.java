package com.collegeeventhub.event.mapper;

import com.collegeeventhub.event.dto.EventRequest;
import com.collegeeventhub.event.dto.EventResponse;
import com.collegeeventhub.event.entity.EventEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class EventMapper {

    public EventEntity toEntity(EventRequest request) {
        return EventEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .venue(request.getVenue())
                .capacity(request.getCapacity())
                .registrationDeadline(request.getRegistrationDeadline())
                .type(request.getType())
                .eligibleBranches(request.getEligibleBranches() == null
                        ? new HashSet<>()
                        : new HashSet<>(request.getEligibleBranches()))
                .eligibleAcademicYears(request.getEligibleAcademicYears() == null
                        ? new HashSet<>()
                        : new HashSet<>(request.getEligibleAcademicYears()))
                .eligibleSections(request.getEligibleSections() == null
                        ? new HashSet<>()
                        : new HashSet<>(request.getEligibleSections()))
                .build();
    }

    public EventResponse toResponse(EventEntity entity) {
        return EventResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .eventDate(entity.getEventDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .venue(entity.getVenue())
                .capacity(entity.getCapacity())
                .registrationDeadline(entity.getRegistrationDeadline())
                .type(entity.getType())
                .status(entity.getStatus())
                .organizationId(entity.getOrganization().getId())
                .organizationName(entity.getOrganization().getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .eligibleBranches(new HashSet<>(entity.getEligibleBranches()))
                .eligibleAcademicYears(new HashSet<>(entity.getEligibleAcademicYears()))
                .eligibleSections(new HashSet<>(entity.getEligibleSections()))
                .build();
    }
}
