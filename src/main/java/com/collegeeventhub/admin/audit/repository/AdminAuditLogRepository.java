package com.collegeeventhub.admin.audit.repository;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.audit.entity.AdminAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository
        extends JpaRepository<AdminAuditLogEntity, Long> {

    @Query("""
            select audit from AdminAuditLogEntity audit
            where (:adminUserId is null or audit.adminUserId = :adminUserId)
              and (:targetType is null or audit.targetType = :targetType)
              and (:action is null or audit.action = :action)
            """)
    Page<AdminAuditLogEntity> findAllByFilters(
            @Param("adminUserId") Long adminUserId,
            @Param("targetType") AdminAuditTargetType targetType,
            @Param("action") AdminAuditAction action,
            Pageable pageable
    );

    List<AdminAuditLogEntity>
    findByAdminUserIdAndActionAndTargetTypeAndTargetIdOrderByIdAsc(
            Long adminUserId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long targetId
    );
}
