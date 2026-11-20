package com.collegeeventhub.organization.repository;

import com.collegeeventhub.organization.entity.OrganizationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

    List<OrganizationEntity> findByLeaderId(Long leaderId);

    @Query("""
            select organization from OrganizationEntity organization
            where (:filterByActive = false or organization.active = :active)
            """)
    Page<OrganizationEntity> findAllByFilters(
            @Param("filterByActive") boolean filterByActive,
            @Param("active") boolean active,
            Pageable pageable
    );

    boolean existsByName(String name);
}
