package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminEventResponse;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.response.PageResponse;

public interface AdminEventService {

    PageResponse<AdminEventResponse> getAllEvents(
            int page,
            int size,
            EventStatus status,
            EventType type,
            Long organizationId
    );

    void cancelEvent(Long eventId, Long adminUserId);
}
