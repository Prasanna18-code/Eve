package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminEventResponse;
import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final AdminAuditLogService adminAuditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminEventResponse> getAllEvents(
            int page,
            int size,
            EventStatus status,
            EventType type,
            Long organizationId) {
        Page<EventEntity> events =
                eventRepository.findAllByListingFilters(
                        false,
                        LocalDate.of(1970, 1, 1),
                        status != null,
                        status == null ? EventStatus.UPCOMING : status,
                        false,
                        false,
                        EventStatus.CANCELLED,
                        EventStatus.DELETED,
                        type != null,
                        type == null ? EventType.ONLINE : type,
                        organizationId != null,
                        organizationId == null ? -1L : organizationId,
                        false,
                        false,
                        "",
                        false,
                        0,
                        false,
                        "",
                        Pagination.pageRequest(
                                page,
                                size,
                                Sort.by(Sort.Order.asc("id"))
                        )
                );
        return PageResponse.from(events, this::toResponse);
    }

    @Override
    @Transactional
    public void cancelEvent(Long eventId, Long adminUserId) {
        EventStatus previousStatus = eventService.cancelEventByAdmin(eventId);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.EVENT_CANCELLED,
                AdminAuditTargetType.EVENT,
                eventId,
                "status changed from " + previousStatus + " to "
                        + EventStatus.CANCELLED
        );
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
