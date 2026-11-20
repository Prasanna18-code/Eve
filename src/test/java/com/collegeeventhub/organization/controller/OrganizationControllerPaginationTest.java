package com.collegeeventhub.organization.controller;

import com.collegeeventhub.common.exception.GlobalExceptionHandler;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import com.collegeeventhub.config.SecurityConfig;
import com.collegeeventhub.organization.dto.OrganizationResponse;
import com.collegeeventhub.organization.service.OrganizationService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        OrganizationControllerPaginationTest.SecurityHandlersConfiguration.class
})
class OrganizationControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void acceptsPaginationAndActiveFilterAndReturnsPageEnvelope() throws Exception {
        when(organizationService.getAllOrganizations(1, 5, false))
                .thenReturn(PageResponse.<OrganizationResponse>builder()
                        .content(List.of())
                        .page(1)
                        .size(5)
                        .totalElements(6)
                        .totalPages(2)
                        .last(true)
                        .build());

        mockMvc.perform(get("/api/v1/organizations?page=1&size=5&active=false")
                        .with(user("leader").roles("CLUB_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(6))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        verify(organizationService).getAllOrganizations(1, 5, false);
    }

    @Test
    void rejectsInvalidPaginationBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/organizations?size=0")
                        .with(user("leader").roles("CLUB_ADMIN")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
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
