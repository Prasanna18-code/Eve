package com.collegeeventhub.feedback.repository;

import com.collegeeventhub.feedback.entity.FeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    Optional<FeedbackEntity> findByEventIdAndUserId(Long eventId, Long userId);

    Page<FeedbackEntity> findByEventId(Long eventId, Pageable pageable);
}
