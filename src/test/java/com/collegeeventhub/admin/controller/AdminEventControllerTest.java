package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.service.AdminEventService;
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

@WebMvcTest(AdminEventController.class)
@Import({
        SecurityConfig.class,
        AdminEventControllerTest.SecurityHandlersConfiguration.class
})
class AdminEventControllerTest {

    private static final List<Endpoint> ENDPOINTS = List.of(
            new Endpoint(HttpMethod.GET, "/api/v1/admin/events"),
            new Endpoint(HttpMethod.PUT, "/api/v1/admin/events/7/cancel")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminEventService adminEventService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void adminCanUseEveryEndpointAndResponsesRemainUnchanged() throws Exception {
        when(adminEventService.getAllEvents()).thenReturn(List.of());

        mockMvc.perform(endpoint(HttpMethod.GET, "/api/v1/admin/events")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Events fetched successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/events/7/cancel"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event cancelled successfully"));

        verify(adminEventService).getAllEvents();
        verify(adminEventService).cancelEvent(7L);
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

        verifyNoInteractions(adminEventService);
    }

    @Test
    void unauthenticatedRequestsAreRejectedForEveryEndpoint() throws Exception {
        for (Endpoint endpoint : ENDPOINTS) {
            mockMvc.perform(endpoint.request())
                    .andExpect(status().isUnauthorized());
        }

        verifyNoInteractions(adminEventService);
    }

    private MockHttpServletRequestBuilder endpoint(
            HttpMethod method,
            String path) {
        return request(method, path);
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
