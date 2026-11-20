package com.collegeeventhub.placement.repository;

import com.collegeeventhub.placement.entity.PlacementApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementApplicationRepository
        extends JpaRepository<PlacementApplicationEntity, Long> {

    boolean existsByPlacementIdAndUserId(Long placementId, Long userId);

    Page<PlacementApplicationEntity> findByPlacementId(
            Long placementId,
            Pageable pageable
    );

    Page<PlacementApplicationEntity> findByUserId(
            Long userId,
            Pageable pageable
    );
}
