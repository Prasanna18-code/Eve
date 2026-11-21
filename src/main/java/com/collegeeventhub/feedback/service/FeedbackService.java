package com.collegeeventhub.feedback.service;

import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.feedback.dto.FeedbackRequest;
import com.collegeeventhub.feedback.dto.FeedbackResponse;

public interface FeedbackService {

    FeedbackResponse createFeedback(
            Long eventId,
            String username,
            FeedbackRequest request
    );

    FeedbackResponse updateMyFeedback(
            Long eventId,
            String username,
            FeedbackRequest request
    );

    FeedbackResponse getMyFeedback(Long eventId, String username);

    PageResponse<FeedbackResponse> getEventFeedback(
            Long eventId,
            String username,
            int page,
            int size
    );
}
