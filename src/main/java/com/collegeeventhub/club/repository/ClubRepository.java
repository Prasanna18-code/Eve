package com.collegeeventhub.club.repository;

import com.collegeeventhub.club.entity.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<ClubEntity, Long> {

    Optional<ClubEntity> findByName(String name);

    boolean existsByName(String name);

    List<ClubEntity> findByOrganizationId(Long organizationId);

    List<ClubEntity> findByLeaderId(Long leaderId);
}
