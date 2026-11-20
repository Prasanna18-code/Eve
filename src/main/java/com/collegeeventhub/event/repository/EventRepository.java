package com.collegeeventhub.event.repository;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.event.entity.EventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @Query(value = """
            select event from EventEntity event
            where (:filterByEventDate = false or event.eventDate >= :eventDateFrom)
              and (:filterByStatus = false or event.status = :status)
              and (:excludeCancelled = false or event.status <> :cancelledStatus)
              and (:excludeDeleted = false or event.status <> :deletedStatus)
              and (:filterByType = false or event.type = :type)
              and (:filterByOrganizationId = false
                  or event.organization.id = :organizationId)
              and (
                  :applyAudienceFilter = false
                  or (
                      (size(event.eligibleBranches) = 0
                          or (:hasBranch = true
                              and :branch member of event.eligibleBranches))
                      and (size(event.eligibleAcademicYears) = 0
                          or (:hasAcademicYear = true
                              and :academicYear member of event.eligibleAcademicYears))
                      and (size(event.eligibleSections) = 0
                          or (:hasSection = true
                              and :section member of event.eligibleSections))
                  )
              )
            """,
            countQuery = """
            select count(event) from EventEntity event
            where (:filterByEventDate = false or event.eventDate >= :eventDateFrom)
              and (:filterByStatus = false or event.status = :status)
              and (:excludeCancelled = false or event.status <> :cancelledStatus)
              and (:excludeDeleted = false or event.status <> :deletedStatus)
              and (:filterByType = false or event.type = :type)
              and (:filterByOrganizationId = false
                  or event.organization.id = :organizationId)
              and (
                  :applyAudienceFilter = false
                  or (
                      (size(event.eligibleBranches) = 0
                          or (:hasBranch = true
                              and :branch member of event.eligibleBranches))
                      and (size(event.eligibleAcademicYears) = 0
                          or (:hasAcademicYear = true
                              and :academicYear member of event.eligibleAcademicYears))
                      and (size(event.eligibleSections) = 0
                          or (:hasSection = true
                              and :section member of event.eligibleSections))
                  )
              )
            """)
    Page<EventEntity> findAllByListingFilters(
            @Param("filterByEventDate") boolean filterByEventDate,
            @Param("eventDateFrom") LocalDate eventDateFrom,
            @Param("filterByStatus") boolean filterByStatus,
            @Param("status") EventStatus status,
            @Param("excludeCancelled") boolean excludeCancelled,
            @Param("excludeDeleted") boolean excludeDeleted,
            @Param("cancelledStatus") EventStatus cancelledStatus,
            @Param("deletedStatus") EventStatus deletedStatus,
            @Param("filterByType") boolean filterByType,
            @Param("type") EventType type,
            @Param("filterByOrganizationId") boolean filterByOrganizationId,
            @Param("organizationId") long organizationId,
            @Param("applyAudienceFilter") boolean applyAudienceFilter,
            @Param("hasBranch") boolean hasBranch,
            @Param("branch") String branch,
            @Param("hasAcademicYear") boolean hasAcademicYear,
            @Param("academicYear") Integer academicYear,
            @Param("hasSection") boolean hasSection,
            @Param("section") String section,
            Pageable pageable
    );

    Optional<EventEntity> findByIdAndStatusNot(Long id, EventStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from EventEntity event "
            + "where event.id = :id and event.status <> :status")
    Optional<EventEntity> findForCancellation(
            @Param("id") Long id,
            @Param("status") EventStatus status
    );

    List<EventEntity> findByEventDateAndStatusNotIn(
            LocalDate eventDate,
            Collection<EventStatus> statuses
    );
}
