package com.collegeeventhub.admin.audit.dto;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogResponse {

    private Long id;
    private Long adminUserId;
    private AdminAuditAction action;
    private AdminAuditTargetType targetType;
    private Long targetId;
    private String details;
    private LocalDateTime createdAt;
}
