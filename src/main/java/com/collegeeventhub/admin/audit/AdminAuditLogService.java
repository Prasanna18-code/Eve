package com.collegeeventhub.admin.audit;

import com.collegeeventhub.admin.audit.dto.AdminAuditLogResponse;
import com.collegeeventhub.common.response.PageResponse;

public interface AdminAuditLogService {

    void record(
            Long adminUserId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long targetId,
            String details
    );

    PageResponse<AdminAuditLogResponse> getAuditLogs(
            int page,
            int size,
            Long adminUserId,
            AdminAuditTargetType targetType,
            AdminAuditAction action
    );
}
