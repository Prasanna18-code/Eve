package com.collegeeventhub.event.dto;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;

    private String title;

    private String description;

    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venue;

    private Integer capacity;

    private LocalDateTime registrationDeadline;

    private EventType type;

    private EventStatus status;

    private Long organizationId;

    private String organizationName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Set<String> eligibleBranches;

    private Set<Integer> eligibleAcademicYears;

    private Set<String> eligibleSections;
}