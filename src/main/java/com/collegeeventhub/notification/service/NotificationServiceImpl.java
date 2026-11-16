package com.collegeeventhub.notification.service;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.dto.NotificationPageResponse;
import com.collegeeventhub.notification.dto.NotificationResponse;
import com.collegeeventhub.notification.entity.NotificationEntity;
import com.collegeeventhub.notification.mapper.NotificationMapper;
import com.collegeeventhub.notification.repository.NotificationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getMyNotifications(
            String username,
            int page,
            int size) {

        UserEntity user = getUser(username);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        Page<NotificationEntity> notificationPage =
                notificationRepository.findByUserId(user.getId(), pageRequest);

        return NotificationPageResponse.builder()
                .content(notificationPage.getContent().stream()
                        .map(notificationMapper::toResponse)
                        .toList())
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {

        UserEntity user = getUser(username);

        return notificationRepository
                .countByUserIdAndReadFalse(user.getId());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String username) {

        UserEntity user = getUser(username);

        NotificationEntity notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to access this notification"
            );
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String username) {

        UserEntity user = getUser(username);

        List<NotificationEntity> notifications =
                notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(user.getId());

        notifications.forEach(notification -> notification.setRead(true));

        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void createNotification(
            Long userId,
            String title,
            String message,
            NotificationType type
    ) {

        createNotification(userId, null, title, message, type);
    }

    @Override
    @Transactional
    public void createNotification(
            Long userId,
            Long eventId,
            String title,
            String message,
            NotificationType type
    ) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        ));

        EventEntity event = null;
        if (eventId != null) {
            event = eventRepository.findById(eventId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Event not found with id: " + eventId
                            ));
        }

        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .event(event)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    private UserEntity getUser(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username
                        ));
    }
}
