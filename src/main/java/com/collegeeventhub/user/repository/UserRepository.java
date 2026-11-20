package com.collegeeventhub.user.repository;

import java.util.Optional;

import com.collegeeventhub.common.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.collegeeventhub.user.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("""
            select user from UserEntity user
            where (:role is null or user.role = :role)
              and (:active is null or user.active = :active)
            """)
    Page<UserEntity> findAllByFilters(
            @Param("role") Role role,
            @Param("active") Boolean active,
            Pageable pageable
    );

    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRollNumberAndIdNot(String rollNumber, Long id);
}
