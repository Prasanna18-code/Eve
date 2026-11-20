package com.collegeeventhub.placement.controller;

import com.collegeeventhub.common.exception.GlobalExceptionHandler;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import com.collegeeventhub.config.SecurityConfig;
import com.collegeeventhub.placement.dto.PlacementRequest;
import com.collegeeventhub.placement.dto.PlacementResponse;
import com.collegeeventhub.placement.entity.PlacementStatus;
import com.collegeeventhub.placement.entity.PlacementType;
import com.collegeeventhub.placement.service.PlacementService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        PlacementController.class,
        AdminPlacementController.class
})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        PlacementControllerTest.SecurityHandlersConfiguration.class
})
class PlacementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlacementService placementService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void publicPlacementBrowsingIsAvailableToAuthenticatedStudents()
            throws Exception {
        when(placementService.getPublishedPlacements(
                0,
                10,
                PlacementType.INTERNSHIP
        )).thenReturn(PageResponse.<PlacementResponse>builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build());

        mockMvc.perform(get("/api/v1/placements?type=INTERNSHIP")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(placementService).getPublishedPlacements(
                0,
                10,
                PlacementType.INTERNSHIP
        );
    }

    @Test
    void onlyAdminsCanCreatePlacementsAndActorIdIsPassedToService()
            throws Exception {
        PlacementRequest request = request();
        PlacementResponse created = PlacementResponse.builder()
                .id(15L)
                .companyName(request.getCompanyName())
                .title(request.getTitle())
                .status(PlacementStatus.DRAFT)
                .build();
        when(placementService.createPlacement(
                org.mockito.ArgumentMatchers.any(PlacementRequest.class),
                org.mockito.ArgumentMatchers.eq(42L)
        )).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/placements")
                        .with(user(principal(42L, "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(15))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/admin/placements")
                        .with(user("student").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/placements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(placementService).createPlacement(
                org.mockito.ArgumentMatchers.any(PlacementRequest.class),
                org.mockito.ArgumentMatchers.eq(42L)
        );
    }

    @Test
    void invalidPaginationIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(get("/api/v1/placements?page=-1")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placementService);
    }

    private PlacementRequest request() {
        return PlacementRequest.builder()
                .companyName("Example Co.")
                .title("Software Engineer")
                .description("Graduate engineering opportunity.")
                .location("Remote")
                .type(PlacementType.FULL_TIME)
                .applicationDeadline(LocalDateTime.now().plusDays(20))
                .build();
    }

    private AuthenticatedUserPrincipal principal(Long id, String role) {
        return new AuthenticatedUserPrincipal(
                id,
                "admin",
                "password",
                role,
                true
        );
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
