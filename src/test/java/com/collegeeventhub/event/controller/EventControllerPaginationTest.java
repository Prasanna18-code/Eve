package com.collegeeventhub.event.controller;

import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.exception.GlobalExceptionHandler;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import com.collegeeventhub.config.SecurityConfig;
import com.collegeeventhub.event.dto.EventResponse;
import com.collegeeventhub.event.service.EventService;
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

@WebMvcTest(EventController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        EventControllerPaginationTest.SecurityHandlersConfiguration.class
})
class EventControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void paginatesAllCancelledAndOrganizationEventListings() throws Exception {
        when(eventService.getAllEvents(1, 5, EventType.ONLINE, 22L))
                .thenReturn(page(1, 5, 6));
        when(eventService.getCancelledEvents(2, 3, null, null))
                .thenReturn(page(2, 3, 7));
        when(eventService.getEventsByOrganization(22L, 0, 10, EventType.HYBRID))
                .thenReturn(page(0, 10, 4));

        mockMvc.perform(get(
                        "/api/v1/events?page=1&size=5&type=ONLINE&organizationId=22"
                ).with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(6));

        mockMvc.perform(get("/api/v1/events/cancelled?page=2&size=3")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(7));

        mockMvc.perform(get(
                        "/api/v1/organizations/22/events?type=HYBRID"
                ).with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(4));

        verify(eventService).getAllEvents(1, 5, EventType.ONLINE, 22L);
        verify(eventService).getCancelledEvents(2, 3, null, null);
        verify(eventService).getEventsByOrganization(22L, 0, 10, EventType.HYBRID);
    }

    @Test
    void rejectsInvalidPaginationBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/events?page=-1")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    private PageResponse<EventResponse> page(
            int page,
            int size,
            long totalElements) {
        return PageResponse.<EventResponse>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / size))
                .last(true)
                .build();
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
