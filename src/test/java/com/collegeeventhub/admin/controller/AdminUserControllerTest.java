package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.service.AdminUserService;
import com.collegeeventhub.admin.dto.AdminUserPageResponse;
import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.common.exception.BadRequestException;
import com.collegeeventhub.common.exception.GlobalExceptionHandler;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.enums.Role;
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
import org.springframework.http.MediaType;
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

@WebMvcTest(AdminUserController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        AdminUserControllerTest.SecurityHandlersConfiguration.class
})
class AdminUserControllerTest {

    private static final List<Endpoint> ENDPOINTS = List.of(
            new Endpoint(HttpMethod.GET, "/api/v1/admin/users"),
            new Endpoint(HttpMethod.PUT, "/api/v1/admin/users/7/activate"),
            new Endpoint(HttpMethod.PUT, "/api/v1/admin/users/7/deactivate"),
            new Endpoint(HttpMethod.PUT, "/api/v1/admin/users/8/role")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void adminCanUseEveryEndpointAndResponsesRemainUnchanged() throws Exception {
        AdminUserResponse updatedUser = AdminUserResponse.builder()
                .id(8L)
                .username("faculty")
                .role(Role.FACULTY)
                .active(true)
                .build();
        when(adminUserService.getAllUsers(0, 10, null, null))
                .thenReturn(pageResponse(List.of(), 0, 10, 0, 0, true));
        when(adminUserService.updateUserRole(8L, Role.FACULTY, 7L))
                .thenReturn(updatedUser);

        mockMvc.perform(endpoint(HttpMethod.GET, "/api/v1/admin/users")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users fetched successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/7/activate"
                ).with(user(principal(7L, "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User activated successfully"));

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/7/deactivate"
                ).with(user(principal(7L, "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deactivated successfully"));

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/8/role"
                ).with(user(principal(7L, "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FACULTY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "User role updated successfully"))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.role").value("FACULTY"));

        verify(adminUserService).getAllUsers(0, 10, null, null);
        verify(adminUserService).activateUser(7L, 7L);
        verify(adminUserService).deactivateUser(7L, 7L);
        verify(adminUserService).updateUserRole(8L, Role.FACULTY, 7L);
    }

    @Test
    void adminCanRequestPagedAndFilteredUsers() throws Exception {
        AdminUserResponse userResponse = AdminUserResponse.builder()
                .id(8L)
                .username("inactive-student")
                .role(Role.STUDENT)
                .active(false)
                .build();
        when(adminUserService.getAllUsers(2, 25, Role.STUDENT, false))
                .thenReturn(pageResponse(
                        List.of(userResponse),
                        2,
                        25,
                        51,
                        3,
                        true
                ));

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/users?page=2&size=25&role=STUDENT&active=false"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(25))
                .andExpect(jsonPath("$.data.totalElements").value(51))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(8))
                .andExpect(jsonPath("$.data.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.data.content[0].active").value(false));

        verify(adminUserService).getAllUsers(2, 25, Role.STUDENT, false);
    }

    @Test
    void invalidPaginationParametersReturnBadRequest() throws Exception {
        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/users?page=-1"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/users?size=51"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(endpoint(
                        HttpMethod.GET,
                        "/api/v1/admin/users?role=NOT_A_ROLE"
                ).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminUserService);
    }

    @Test
    void roleUpdateRequiresAValidRole() throws Exception {
        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/8/role"
                ).with(user(principal(7L, "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":null}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/8/role"
                ).with(user(principal(7L, "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"NOT_A_ROLE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminUserService);
    }

    @Test
    void roleUpdateNotFoundReturnsExistingErrorResponse() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(adminUserService).updateUserRole(404L, Role.FACULTY, 7L);

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/404/role"
                ).with(user(principal(7L, "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FACULTY\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(adminUserService).updateUserRole(404L, Role.FACULTY, 7L);
    }

    @Test
    void selfDeactivationReturnsBadRequestWithErrorResponse() throws Exception {
        doThrow(new BadRequestException("You cannot deactivate your own account"))
                .when(adminUserService).deactivateUser(7L, 7L);

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/7/deactivate"
                ).with(user(principal(7L, "admin"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "You cannot deactivate your own account"));

        verify(adminUserService).deactivateUser(7L, 7L);
    }

    @Test
    void deactivationNotFoundReturnsExistingErrorResponse() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(adminUserService).deactivateUser(404L, 7L);

        mockMvc.perform(endpoint(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/404/deactivate"
                ).with(user(principal(7L, "admin"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(adminUserService).deactivateUser(404L, 7L);
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

        verifyNoInteractions(adminUserService);
    }

    @Test
    void unauthenticatedRequestsAreRejectedForEveryEndpoint() throws Exception {
        for (Endpoint endpoint : ENDPOINTS) {
            mockMvc.perform(endpoint.request())
                    .andExpect(status().isUnauthorized());
        }

        verifyNoInteractions(adminUserService);
    }

    private MockHttpServletRequestBuilder endpoint(
            HttpMethod method,
            String path) {
        return request(method, path);
    }

    private AuthenticatedUserPrincipal principal(Long id, String username) {
        return new AuthenticatedUserPrincipal(
                id,
                username,
                "password",
                "ADMIN",
                true
        );
    }

    private AdminUserPageResponse pageResponse(
            List<AdminUserResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last) {
        return AdminUserPageResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    private record Endpoint(HttpMethod method, String path) {
        private MockHttpServletRequestBuilder request() {
            MockHttpServletRequestBuilder request =
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .request(method, path);
            if (path.endsWith("/role")) {
                request.contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FACULTY\"}");
            }
            return request;
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
