package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.service.AdminOrganizationService;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import com.collegeeventhub.config.SecurityConfig;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;
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

@WebMvcTest(AdminOrganizationController.class)
@Import({
        SecurityConfig.class,
        AdminOrganizationControllerTest.SecurityHandlersConfiguration.class
})
class AdminOrganizationControllerTest {

    private static final List<Endpoint> ENDPOINTS = List.of(
            new Endpoint(HttpMethod.GET, "/api/v1/admin/organizations"),
            new Endpoint(HttpMethod.PUT, "/api/v1/admin/organizations/7/activate"),
            new Endpoint(HttpMethod.PUT, "/api/v1/admin/organizations/7/deactivate")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOrganizationService adminOrganizationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void adminCanUseEveryEndpointAndResponsesRemainUnchanged() throws Exception {
        when(adminOrganizationService.getAllOrganizations(0, 10, null))
                .thenReturn(PageResponse.<com.collegeeventhub.admin.dto.AdminOrganizationResponse>
                        builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .last(true)
                        .build());

        mockMvc.perform(endpoint(HttpMethod.GET, "/api/v1/admin/organizations")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Organizations fetched successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/organizations/7/activate"
                ).with(user(principal(7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Organization activated successfully"));

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/organizations/7/deactivate"
                ).with(user(principal(7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Organization deactivated successfully"));

        verify(adminOrganizationService).getAllOrganizations(0, 10, null);
        verify(adminOrganizationService).activateOrganization(7L, 7L);
        verify(adminOrganizationService).deactivateOrganization(7L, 7L);
    }

    @Test
    void adminCanFilterOrganizationsAndInvalidPaginationIsRejected() throws Exception {
        when(adminOrganizationService.getAllOrganizations(2, 25, false))
                .thenReturn(PageResponse.<com.collegeeventhub.admin.dto.AdminOrganizationResponse>
                        builder()
                        .content(List.of())
                        .page(2)
                        .size(25)
                        .totalElements(51)
                        .totalPages(3)
                        .last(true)
                        .build());

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/organizations?page=2&size=25&active=false"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(51));

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/organizations?page=-1"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/organizations?size=51"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        verify(adminOrganizationService).getAllOrganizations(2, 25, false);
    }

    @Test
    void studentAndClubAdminAreForbiddenFromEveryEndpoint() throws Exception {
        for (String role : List.of("STUDENT", "CLUB_ADMIN")) {
            for (Endpoint endpoint : ENDPOINTS) {
                mockMvc.perform(endpoint.request()
                                .with(user("nonAdmin").roles(role)))
                        .andExpect(status().isForbidden());
            }
        }

        verifyNoInteractions(adminOrganizationService);
    }

    @Test
    void unauthenticatedRequestsAreRejectedForEveryEndpoint() throws Exception {
        for (Endpoint endpoint : ENDPOINTS) {
            mockMvc.perform(endpoint.request())
                    .andExpect(status().isUnauthorized());
        }

        verifyNoInteractions(adminOrganizationService);
    }

    private MockHttpServletRequestBuilder endpoint(
            HttpMethod method,
            String path) {
        return request(method, path);
    }

    private AuthenticatedUserPrincipal principal(Long id) {
        return new AuthenticatedUserPrincipal(
                id,
                "admin",
                "password",
                "ADMIN",
                true
        );
    }

    private record Endpoint(HttpMethod method, String path) {
        private MockHttpServletRequestBuilder request() {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .request(method, path);
        }
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
