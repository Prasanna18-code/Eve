package com.collegeeventhub.club.repository;

import com.collegeeventhub.club.entity.ClubEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<ClubEntity, Long> {

    @Query("""
            select club from ClubEntity club
            where (:filterByActive = false or club.active = :active)
            """)
    Page<ClubEntity> findAllByFilters(
            @Param("filterByActive") boolean filterByActive,
            @Param("active") boolean active,
            Pageable pageable
    );

    Optional<ClubEntity> findByName(String name);

    boolean existsByName(String name);

    List<ClubEntity> findByOrganizationId(Long organizationId);

    List<ClubEntity> findByLeaderId(Long leaderId);
}
