package com.collegeeventhub.notification.controller;

import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import com.collegeeventhub.config.SecurityConfig;
import com.collegeeventhub.notification.entity.NotificationPreferenceEntity;
import com.collegeeventhub.notification.service.NotificationPreferenceService;
import com.collegeeventhub.security.CustomUserDetailsService;
import com.collegeeventhub.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationPreferenceController.class)
@Import({
        SecurityConfig.class,
        NotificationPreferenceControllerTest.SecurityHandlersConfiguration.class
})
class NotificationPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationPreferenceService preferenceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void getPreferencesReturnsAllEnabledDefaults() throws Exception {
        when(preferenceService.getPreferences("student1"))
                .thenReturn(preferences(true, true, true, true));

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .with(user("student1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationSuccessful").value(true))
                .andExpect(jsonPath("$.data.eventUpdated").value(true))
                .andExpect(jsonPath("$.data.eventCancelled").value(true))
                .andExpect(jsonPath("$.data.eventReminder").value(true));

        verify(preferenceService).getPreferences("student1");
    }

    @Test
    void getPreferencesReturnsSavedPreferencesForAuthenticatedUser() throws Exception {
        when(preferenceService.getPreferences("student1"))
                .thenReturn(preferences(true, false, true, false));

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .with(user("student1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationSuccessful").value(true))
                .andExpect(jsonPath("$.data.eventUpdated").value(false))
                .andExpect(jsonPath("$.data.eventCancelled").value(true))
                .andExpect(jsonPath("$.data.eventReminder").value(false));

        verify(preferenceService).getPreferences("student1");
    }

    @Test
    void putPreferencesCreatesPreferencesForFirstUpdate() throws Exception {
        when(preferenceService.updatePreferences(
                "student1", true, false, true, false
        )).thenReturn(preferences(true, false, true, false));

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .with(user("student1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceJson(true, false, true, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationSuccessful").value(true))
                .andExpect(jsonPath("$.data.eventUpdated").value(false))
                .andExpect(jsonPath("$.data.eventCancelled").value(true))
                .andExpect(jsonPath("$.data.eventReminder").value(false));

        verify(preferenceService).updatePreferences(
                "student1", true, false, true, false
        );
    }

    @Test
    void putPreferencesUpdatesExistingPreferences() throws Exception {
        when(preferenceService.updatePreferences(
                "student1", false, true, false, true
        )).thenReturn(preferences(false, true, false, true));

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .with(user("student1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferenceJson(false, true, false, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationSuccessful").value(false))
                .andExpect(jsonPath("$.data.eventUpdated").value(true))
                .andExpect(jsonPath("$.data.eventCancelled").value(false))
                .andExpect(jsonPath("$.data.eventReminder").value(true));

        verify(preferenceService).updatePreferences(
                "student1", false, true, false, true
        );
    }

    @Test
    void authenticatedUserCannotSelectAnotherUsersPreferences() throws Exception {
        when(preferenceService.updatePreferences(
                "student1", false, false, false, false
        )).thenReturn(preferences(false, false, false, false));

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .with(user("student1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 2,
                                  "registrationSuccessful": false,
                                  "eventUpdated": false,
                                  "eventCancelled": false,
                                  "eventReminder": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationSuccessful").value(false));

        verify(preferenceService).updatePreferences(
                "student1", false, false, false, false
        );
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(preferenceService);
    }

    private NotificationPreferenceEntity preferences(
            boolean registrationSuccessful,
            boolean eventUpdated,
            boolean eventCancelled,
            boolean eventReminder) {

        return NotificationPreferenceEntity.builder()
                .registrationSuccessful(registrationSuccessful)
                .eventUpdated(eventUpdated)
                .eventCancelled(eventCancelled)
                .eventReminder(eventReminder)
                .build();
    }

    private String preferenceJson(
            boolean registrationSuccessful,
            boolean eventUpdated,
            boolean eventCancelled,
            boolean eventReminder) {
        return """
                {
                  "registrationSuccessful": %s,
                  "eventUpdated": %s,
                  "eventCancelled": %s,
                  "eventReminder": %s
                }
                """.formatted(
                registrationSuccessful,
                eventUpdated,
                eventCancelled,
                eventReminder
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
