package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminEventResponse;

import java.util.List;

public interface AdminEventService {

    List<AdminEventResponse> getAllEvents();

    void cancelEvent(Long eventId);
}
