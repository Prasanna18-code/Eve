package com.collegeeventhub.feedback.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.feedback.dto.FeedbackRequest;
import com.collegeeventhub.feedback.dto.FeedbackResponse;
import com.collegeeventhub.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/{eventId}/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @PathVariable Long eventId,
            Authentication authentication,
            @Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse feedback = feedbackService.createFeedback(
                eventId,
                authentication.getName(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Feedback submitted successfully",
                feedback
        ));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateMyFeedback(
            @PathVariable Long eventId,
            Authentication authentication,
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Feedback updated successfully",
                feedbackService.updateMyFeedback(
                        eventId,
                        authentication.getName(),
                        request
                )
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getMyFeedback(
            @PathVariable Long eventId,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Feedback fetched successfully",
                feedbackService.getMyFeedback(
                        eventId,
                        authentication.getName()
                )
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>>
    getEventFeedback(
            @PathVariable Long eventId,
            Authentication authentication,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size) {
        Pagination.validate(page, size);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Event feedback fetched successfully",
                feedbackService.getEventFeedback(
                        eventId,
                        authentication.getName(),
                        page,
                        size
                )
        ));
    }
}
