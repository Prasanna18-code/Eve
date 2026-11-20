package com.collegeeventhub.admin.audit;

import com.collegeeventhub.admin.audit.dto.AdminAuditLogResponse;
import com.collegeeventhub.admin.audit.entity.AdminAuditLogEntity;
import com.collegeeventhub.admin.audit.repository.AdminAuditLogRepository;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            Long adminUserId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long targetId,
            String details) {
        AdminAuditLogEntity auditLog = AdminAuditLogEntity.builder()
                .adminUserId(adminUserId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        adminAuditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> getAuditLogs(
            int page,
            int size,
            Long adminUserId,
            AdminAuditTargetType targetType,
            AdminAuditAction action) {
        Page<AdminAuditLogEntity> auditLogs =
                adminAuditLogRepository.findAllByFilters(
                        adminUserId,
                        targetType,
                        action,
                        Pagination.pageRequest(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.desc("id")
                                )
                        )
                );
        return PageResponse.from(auditLogs, this::toResponse);
    }

    private AdminAuditLogResponse toResponse(AdminAuditLogEntity auditLog) {
        return AdminAuditLogResponse.builder()
                .id(auditLog.getId())
                .adminUserId(auditLog.getAdminUserId())
                .action(auditLog.getAction())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
