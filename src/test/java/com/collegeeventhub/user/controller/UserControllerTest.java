package com.collegeeventhub.user.controller;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import com.collegeeventhub.config.SecurityConfig;
import com.collegeeventhub.security.CustomUserDetailsService;
import com.collegeeventhub.security.JwtService;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;
import com.collegeeventhub.user.dto.UserResponse;
import com.collegeeventhub.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        UserControllerTest.SecurityHandlersConfiguration.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void studentCanReadOwnProfile() throws Exception {
        UserResponse response = response(42L, "student42");
        when(userService.getUserById(42L, 42L, false))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/users/42")
                        .with(user(principal(42L, "student42", "STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("student42"));

        verify(userService).getUserById(42L, 42L, false);
    }

    @Test
    void studentCannotReadAnotherUsersProfile() throws Exception {
        when(userService.getUserById(42L, 75L, false))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/users/42")
                        .with(user(principal(75L, "student1", "STUDENT"))))
                .andExpect(status().isForbidden());

        verify(userService).getUserById(42L, 75L, false);
    }

    @Test
    void clubAdminCannotReadAnotherUsersProfile() throws Exception {
        when(userService.getUserById(42L, 75L, false))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/users/42")
                        .with(user(principal(75L, "clubAdmin", "CLUB_ADMIN"))))
                .andExpect(status().isForbidden());

        verify(userService).getUserById(42L, 75L, false);
    }

    @Test
    void adminCanReadAnotherUsersProfile() throws Exception {
        UserResponse response = response(42L, "student42");
        when(userService.getUserById(42L, 75L, true))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/users/42")
                        .with(user(principal(75L, "platformAdmin", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("student42"));

        verify(userService).getUserById(42L, 75L, true);
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/42"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void nonexistentProfileStillReturnsNotFound() throws Exception {
        when(userService.getUserById(404L, 42L, false))
                .thenThrow(new ResourceNotFoundException("User not found with id: 404"));

        mockMvc.perform(get("/api/v1/users/404")
                        .with(user(principal(42L, "student42", "STUDENT"))))
                .andExpect(status().isNotFound());

        verify(userService).getUserById(404L, 42L, false);
    }

    private UserResponse response(Long id, String username) {
        return UserResponse.builder()
                .id(id)
                .username(username)
                .build();
    }

    private AuthenticatedUserPrincipal principal(
            Long id,
            String username,
            String role) {
        return new AuthenticatedUserPrincipal(
                id,
                username,
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
