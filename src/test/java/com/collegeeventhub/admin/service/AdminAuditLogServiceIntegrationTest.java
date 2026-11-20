package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.audit.dto.AdminAuditLogResponse;
import com.collegeeventhub.common.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@Rollback
class AdminAuditLogServiceIntegrationTest {

    @Autowired
    private AdminAuditLogService adminAuditLogService;

    @Test
    void recordsActionsAndFiltersPagedAuditHistory() {
        adminAuditLogService.record(
                7001L,
                AdminAuditAction.CLUB_DEACTIVATED,
                AdminAuditTargetType.CLUB,
                8001L,
                "active changed from true to false"
        );
        adminAuditLogService.record(
                7001L,
                AdminAuditAction.USER_ROLE_CHANGED,
                AdminAuditTargetType.USER,
                8002L,
                "role changed from STUDENT to FACULTY"
        );
        adminAuditLogService.record(
                7002L,
                AdminAuditAction.CLUB_ACTIVATED,
                AdminAuditTargetType.CLUB,
                8003L,
                "active changed from false to true"
        );

        PageResponse<AdminAuditLogResponse> page =
                adminAuditLogService.getAuditLogs(
                        0,
                        1,
                        7001L,
                        AdminAuditTargetType.CLUB,
                        null
                );

        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getTotalElements());
        assertEquals(AdminAuditAction.CLUB_DEACTIVATED,
                page.getContent().get(0).getAction());
        assertEquals(AdminAuditTargetType.CLUB,
                page.getContent().get(0).getTargetType());
        assertEquals("active changed from true to false",
                page.getContent().get(0).getDetails());
        assertEquals(7001L, page.getContent().get(0).getAdminUserId());
    }
}
