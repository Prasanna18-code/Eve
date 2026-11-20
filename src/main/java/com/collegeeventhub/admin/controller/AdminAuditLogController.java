package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.audit.dto.AdminAuditLogResponse;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminAuditLogResponse>>>
    getAuditLogs(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) Long adminUserId,
            @RequestParam(required = false) AdminAuditTargetType targetType,
            @RequestParam(required = false) AdminAuditAction action) {
        Pagination.validate(page, size);
        PageResponse<AdminAuditLogResponse> auditLogs =
                adminAuditLogService.getAuditLogs(
                        page,
                        size,
                        adminUserId,
                        targetType,
                        action
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Audit logs fetched successfully",
                        auditLogs
                )
        );
    }
}
