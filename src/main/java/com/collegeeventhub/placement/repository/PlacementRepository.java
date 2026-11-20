package com.collegeeventhub.placement.repository;

import com.collegeeventhub.placement.entity.PlacementEntity;
import com.collegeeventhub.placement.entity.PlacementStatus;
import com.collegeeventhub.placement.entity.PlacementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PlacementRepository extends JpaRepository<PlacementEntity, Long> {

    @Query("""
            select placement from PlacementEntity placement
            where (:filterByStatus = false or placement.status = :status)
              and (:filterByType = false or placement.type = :type)
            """)
    Page<PlacementEntity> findAllByFilters(
            @Param("filterByStatus") boolean filterByStatus,
            @Param("status") PlacementStatus status,
            @Param("filterByType") boolean filterByType,
            @Param("type") PlacementType type,
            Pageable pageable
    );

    @Query("""
            select placement from PlacementEntity placement
            where placement.status = :status
              and placement.applicationDeadline > :now
              and (:filterByType = false or placement.type = :type)
            """)
    Page<PlacementEntity> findPublishedByType(
            @Param("status") PlacementStatus status,
            @Param("now") LocalDateTime now,
            @Param("filterByType") boolean filterByType,
            @Param("type") PlacementType type,
            Pageable pageable
    );
}
