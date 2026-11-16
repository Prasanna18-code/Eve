package com.collegeeventhub.notification.controller;

import com.collegeeventhub.notification.dto.NotificationPageResponse;
import com.collegeeventhub.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private final NotificationService notificationService =
            mock(NotificationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .build();
    }

    @Test
    void getMyNotificationsUsesDefaultPageAndSize() throws Exception {
        when(notificationService.getMyNotifications("student1", 0, 10))
                .thenReturn(pageResponse(0, 10, 0, 0, true));

        performGet(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.last").value(true));

        verify(notificationService).getMyNotifications("student1", 0, 10);
    }

    @Test
    void getMyNotificationsUsesRequestedPageAndSize() throws Exception {
        when(notificationService.getMyNotifications("student1", 2, 5))
                .thenReturn(pageResponse(2, 5, 12, 3, true));

        performGet(get("/api/v1/notifications").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").value(true));

        verify(notificationService).getMyNotifications("student1", 2, 5);
    }

    @Test
    void getMyNotificationsRejectsInvalidPageAndSize() throws Exception {
        performGet(get("/api/v1/notifications").param("page", "-1"))
                .andExpect(status().isBadRequest());
        performGet(get("/api/v1/notifications").param("size", "-1"))
                .andExpect(status().isBadRequest());
        performGet(get("/api/v1/notifications").param("size", "0"))
                .andExpect(status().isBadRequest());
        performGet(get("/api/v1/notifications").param("size", "51"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    private org.springframework.test.web.servlet.ResultActions performGet(
            MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request
                .principal(new UsernamePasswordAuthenticationToken(
                        "student1",
                        null
                ))
                .accept(MediaType.APPLICATION_JSON));
    }

    private NotificationPageResponse pageResponse(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last) {

        return NotificationPageResponse.builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }
}
