package com.collegeeventhub.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageResponse {

    private List<NotificationResponse> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean last;
}
