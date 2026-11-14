package com.collegeeventhub.registration.repository;

import com.collegeeventhub.registration.entity.RegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository
        extends JpaRepository<RegistrationEntity, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<RegistrationEntity> findByUserIdAndEventId(
            Long userId,
            Long eventId
    );

    List<RegistrationEntity> findByUserId(Long userId);

    List<RegistrationEntity> findByEventId(Long eventId);

    long countByEventId(Long eventId);
}
