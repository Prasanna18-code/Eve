package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
import com.collegeeventhub.admin.audit.dto.AdminAuditLogResponse;
import com.collegeeventhub.common.exception.GlobalExceptionHandler;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import com.collegeeventhub.config.SecurityConfig;
import com.collegeeventhub.security.CustomUserDetailsService;
import com.collegeeventhub.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditLogController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        AdminAuditLogControllerTest.SecurityHandlersConfiguration.class
})
class AdminAuditLogControllerTest {

    private static final String PATH = "/api/v1/admin/audit-logs";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAuditLogService adminAuditLogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void adminCanFetchPaginatedAndFilteredAuditLogs() throws Exception {
        AdminAuditLogResponse auditLog = AdminAuditLogResponse.builder()
                .id(1L)
                .adminUserId(7L)
                .action(AdminAuditAction.CLUB_DEACTIVATED)
                .targetType(AdminAuditTargetType.CLUB)
                .targetId(23L)
                .details("active changed from true to false")
                .build();
        when(adminAuditLogService.getAuditLogs(
                1,
                20,
                7L,
                AdminAuditTargetType.CLUB,
                AdminAuditAction.CLUB_DEACTIVATED
        )).thenReturn(PageResponse.<AdminAuditLogResponse>builder()
                .content(List.of(auditLog))
                .page(1)
                .size(20)
                .totalElements(21)
                .totalPages(2)
                .last(true)
                .build());

        mockMvc.perform(request(
                        HttpMethod.GET,
                        PATH + "?page=1&size=20&adminUserId=7"
                                + "&targetType=CLUB&action=CLUB_DEACTIVATED"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(21))
                .andExpect(jsonPath("$.data.content[0].adminUserId").value(7))
                .andExpect(jsonPath("$.data.content[0].action")
                        .value("CLUB_DEACTIVATED"))
                .andExpect(jsonPath("$.data.content[0].targetType").value("CLUB"))
                .andExpect(jsonPath("$.data.content[0].targetId").value(23));

        verify(adminAuditLogService).getAuditLogs(
                1,
                20,
                7L,
                AdminAuditTargetType.CLUB,
                AdminAuditAction.CLUB_DEACTIVATED
        );
    }

    @Test
    void invalidPaginationAndFiltersAreRejected() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, PATH + "?page=-1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(request(HttpMethod.GET, PATH + "?action=UNKNOWN")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminAuditLogService);
    }

    @Test
    void nonAdminAndUnauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, PATH)
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(request(HttpMethod.GET, PATH))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminAuditLogService);
    }

    @TestConfiguration
    static class SecurityHandlersConfiguration {

        @Bean
        CustomAuthenticationEntryPoint authenticationEntryPoint(
                ObjectMapper objectMapper) {
            return new CustomAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        CustomAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
            return new CustomAccessDeniedHandler(objectMapper);
        }
    }
}
