package com.collegeeventhub.registration.repository;

import com.collegeeventhub.common.enums.RegistrationStatus;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository
        extends JpaRepository<RegistrationEntity, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    boolean existsByUserIdAndEventIdAndStatus(
            Long userId,
            Long eventId,
            RegistrationStatus status
    );

    Optional<RegistrationEntity> findByUserIdAndEventId(
            Long userId,
            Long eventId
    );

    List<RegistrationEntity> findByUserId(Long userId);

    List<RegistrationEntity> findByEventId(Long eventId);

    long countByEventId(Long eventId);
}
