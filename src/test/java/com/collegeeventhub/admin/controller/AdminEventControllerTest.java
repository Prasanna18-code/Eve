package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.service.AdminEventService;
import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.exception.GlobalExceptionHandler;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
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
        when(adminEventService.getAllEvents(0, 10, null, null, null))
                .thenReturn(PageResponse.<com.collegeeventhub.admin.dto.AdminEventResponse>
                        builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .last(true)
                        .build());

        mockMvc.perform(endpoint(HttpMethod.GET, "/api/v1/admin/events")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Events fetched successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/events/7/cancel"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event cancelled successfully"));

        verify(adminEventService).getAllEvents(0, 10, null, null, null);
        verify(adminEventService).cancelEvent(7L);
    }

    @Test
    void adminCanFilterEventsAndInvalidPaginationIsRejected() throws Exception {
        when(adminEventService.getAllEvents(
                1,
                20,
                EventStatus.UPCOMING,
                EventType.OFFLINE,
                3L
        )).thenReturn(PageResponse.<com.collegeeventhub.admin.dto.AdminEventResponse>
                builder()
                .content(List.of())
                .page(1)
                .size(20)
                .totalElements(21)
                .totalPages(2)
                .last(true)
                .build());

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/events?page=1&size=20&status=UPCOMING"
                                + "&type=OFFLINE&organizationId=3"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(21));

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/events?size=0"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        verify(adminEventService).getAllEvents(
                1,
                20,
                EventStatus.UPCOMING,
                EventType.OFFLINE,
                3L
        );
    }

    @Test
    void invalidEventIdReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Event not found with id: 404"))
                .when(adminEventService).cancelEvent(404L);

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/events/404/cancel"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Event not found with id: 404"));

        verify(adminEventService).cancelEvent(404L);
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
