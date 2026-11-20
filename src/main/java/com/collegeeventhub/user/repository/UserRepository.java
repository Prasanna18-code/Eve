package com.collegeeventhub.user.repository;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.user.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user from UserEntity user
            where user.role = :role and user.active = true
            order by user.id
            """)
    List<UserEntity> findActiveUsersByRoleForUpdate(
            @Param("role") Role role
    );

    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRollNumberAndIdNot(String rollNumber, Long id);
}
