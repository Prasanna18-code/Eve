package com.collegeeventhub.organization.repository;

import com.collegeeventhub.organization.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

    List<OrganizationEntity> findByLeaderId(Long leaderId);

    boolean existsByName(String name);
}
