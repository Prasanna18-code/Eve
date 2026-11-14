package com.collegeeventhub.notification.repository;

import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    boolean existsByUserIdAndEventIdAndType(
            Long userId,
            Long eventId,
            NotificationType type
    );
}
