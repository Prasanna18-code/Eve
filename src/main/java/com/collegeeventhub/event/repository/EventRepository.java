package com.collegeeventhub.event.repository;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.event.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByOrganizationIdAndStatusNot(
            Long organizationId,
            EventStatus status
    );

    Optional<EventEntity> findByIdAndStatusNot(Long id, EventStatus status);

    List<EventEntity> findByEventDateAndStatusNotIn(
            LocalDate eventDate,
            Collection<EventStatus> statuses
    );

    List<EventEntity> findByEventDateGreaterThanEqualAndStatusNotInOrderByEventDateAsc(
            LocalDate eventDate,
            Collection<EventStatus> statuses
    );

    List<EventEntity> findByEventDateGreaterThanEqualAndStatusOrderByEventDateAsc(
            LocalDate eventDate,
            EventStatus status
    );
}
