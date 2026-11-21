package com.collegeeventhub.feedback.service;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.RegistrationStatus;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.StudentProfileVerificationGuard;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.feedback.dto.FeedbackRequest;
import com.collegeeventhub.feedback.dto.FeedbackResponse;
import com.collegeeventhub.feedback.entity.FeedbackEntity;
import com.collegeeventhub.feedback.repository.FeedbackRepository;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final StudentProfileVerificationGuard profileVerificationGuard;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(
            Long eventId,
            String username,
            FeedbackRequest request) {
        UserEntity user = findUser(username);
        checkStudent(user);
        EventEntity event = findEvent(eventId);
        checkFeedbackEligibility(event, user);
        if (feedbackRepository.findByEventIdAndUserId(eventId, user.getId())
                .isPresent()) {
            throw new ConflictException(
                    "You have already submitted feedback for this event");
        }

        FeedbackEntity feedback = FeedbackEntity.builder()
                .event(event)
                .user(user)
                .rating(request.getRating())
                .comment(normalizeComment(request.getComment()))
                .build();
        try {
            return toResponse(feedbackRepository.saveAndFlush(feedback));
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueConstraintViolation(exception)) {
                throw new ConflictException(
                        "You have already submitted feedback for this event");
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public FeedbackResponse updateMyFeedback(
            Long eventId,
            String username,
            FeedbackRequest request) {
        UserEntity user = findUser(username);
        checkStudent(user);
        EventEntity event = findEvent(eventId);
        checkFeedbackEligibility(event, user);
        FeedbackEntity feedback = feedbackRepository
                .findByEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Feedback not found for this event"));
        feedback.setRating(request.getRating());
        feedback.setComment(normalizeComment(request.getComment()));
        return toResponse(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getMyFeedback(Long eventId, String username) {
        UserEntity user = findUser(username);
        checkStudent(user);
        return feedbackRepository.findByEventIdAndUserId(eventId, user.getId())
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Feedback not found for this event"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponse> getEventFeedback(
            Long eventId,
            String username,
            int page,
            int size) {
        UserEntity user = findUser(username);
        EventEntity event = findEvent(eventId);
        if (user.getRole() != Role.ADMIN
                && !event.getOrganization().getLeader().getId()
                .equals(user.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to view feedback for this event");
        }
        Page<FeedbackEntity> feedback = feedbackRepository.findByEventId(
                eventId,
                Pagination.pageRequest(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                )
        );
        return PageResponse.from(feedback, this::toResponse);
    }

    private void checkFeedbackEligibility(EventEntity event, UserEntity user) {
        if (event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.DELETED) {
            throw new ConflictException(
                    "Feedback is not available for cancelled or deleted events");
        }
        LocalDateTime eventEnd = LocalDateTime.of(
                event.getEventDate(),
                event.getEndTime()
        );
        if (event.getStatus() != EventStatus.COMPLETED
                && eventEnd.isAfter(LocalDateTime.now())) {
            throw new ConflictException(
                    "Feedback can only be submitted after the event");
        }
        if (!registrationRepository.existsByUserIdAndEventIdAndStatus(
                user.getId(),
                event.getId(),
                RegistrationStatus.REGISTERED
        )) {
            throw new AccessDeniedException(
                    "You must be registered for the event to submit feedback");
        }
    }

    private void checkStudent(UserEntity user) {
        if (user.getRole() != Role.STUDENT) {
            throw new AccessDeniedException(
                    "Only students can submit or view their feedback");
        }
        profileVerificationGuard.checkUser(user);
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private EventEntity findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + eventId));
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private FeedbackResponse toResponse(FeedbackEntity feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .eventId(feedback.getEvent().getId())
                .eventTitle(feedback.getEvent().getTitle())
                .userId(feedback.getUser().getId())
                .username(feedback.getUser().getUsername())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
