package com.collegeeventhub.notification;

import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.notification.entity.NotificationEntity;
import com.collegeeventhub.notification.entity.NotificationPreferenceEntity;
import com.collegeeventhub.notification.repository.NotificationRepository;
import com.collegeeventhub.notification.repository.NotificationPreferenceRepository;
import com.collegeeventhub.notification.scheduler.NotificationScheduler;
import com.collegeeventhub.notification.service.NotificationService;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class NotificationPreferenceIntegrationTest {

    private static final String PREFERENCES_PATH =
            "/api/v1/notifications/preferences";
    private static final String NOTIFICATIONS_PATH = "/api/v1/notifications";
    private static final String PASSWORD = "IntegrationPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private NotificationScheduler notificationScheduler;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void preferencesControlReminderCreationAndRemainUserSpecific() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AuthenticatedUser firstUser = registerAndLogin("first_" + suffix);
        AuthenticatedUser secondUser = registerAndLogin("second_" + suffix);

        assertAllEnabled(getPreferences(firstUser.token()));
        assertAllEnabled(getPreferences(secondUser.token()));

        notificationService.createNotification(
                firstUser.user().getId(),
                "Existing notification",
                "Created before reminders were disabled",
                NotificationType.REGISTRATION_SUCCESSFUL
        );
        updatePreferences(firstUser.token(), true, true, true, false);
        entityManager.flush();
        entityManager.clear();

        JsonNode persistedDisabledPreferences = getPreferences(firstUser.token());
        assertTrue(persistedDisabledPreferences.get("eventUpdated").asBoolean());
        assertFalse(persistedDisabledPreferences.get("eventReminder").asBoolean());
        assertAllEnabled(getPreferences(secondUser.token()));

        EventEntity event = createReminderEvent(firstUser.user(), suffix);
        registrationRepository.saveAll(List.of(
                RegistrationEntity.builder()
                        .user(firstUser.user())
                        .event(event)
                        .build(),
                RegistrationEntity.builder()
                        .user(secondUser.user())
                        .event(event)
                        .build()
        ));
        entityManager.flush();

        notificationScheduler.sendEventReminders();

        assertEquals(0, reminderCount(firstUser.user().getId(), event.getId()));
        assertEquals(1, reminderCount(secondUser.user().getId(), event.getId()));
        assertTrue(hasNotification(firstUser.token(),
                NotificationType.REGISTRATION_SUCCESSFUL, null));
        assertFalse(hasNotification(firstUser.token(),
                NotificationType.EVENT_REMINDER, event.getId()));
        assertTrue(hasNotification(secondUser.token(),
                NotificationType.EVENT_REMINDER, event.getId()));
        assertAllEnabled(getPreferences(secondUser.token()));

        updatePreferences(firstUser.token(), true, true, true, true);
        notificationScheduler.sendEventReminders();
        entityManager.flush();

        assertEquals(1, reminderCount(firstUser.user().getId(), event.getId()));
        assertEquals(1, reminderCount(secondUser.user().getId(), event.getId()));
        assertTrue(hasNotification(firstUser.token(),
                NotificationType.EVENT_REMINDER, event.getId()));
        assertTrue(hasNotification(secondUser.token(),
                NotificationType.EVENT_REMINDER, event.getId()));

        entityManager.clear();
        assertAllEnabled(getPreferences(firstUser.token()));
        assertAllEnabled(getPreferences(secondUser.token()));
    }

    @Test
    void databaseAllowsOnlyOnePreferenceRecordPerUser() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AuthenticatedUser authenticatedUser =
                registerAndLogin("unique_" + suffix);

        preferenceRepository.saveAndFlush(
                NotificationPreferenceEntity.builder()
                        .user(authenticatedUser.user())
                        .build()
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> preferenceRepository.saveAndFlush(
                        NotificationPreferenceEntity.builder()
                                .user(authenticatedUser.user())
                                .build()
                )
        );
    }

    private AuthenticatedUser registerAndLogin(String username) throws Exception {
        String registerBody = """
                {
                  "username": "%s",
                  "email": "%s@example.com",
                  "password": "%s",
                  "firstName": "Integration",
                  "lastName": "Test"
                }
                """.formatted(username, username, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        ).path("data").path("token").asText();
        UserEntity user = userRepository.findByUsername(username).orElseThrow();
        return new AuthenticatedUser(user, token);
    }

    private JsonNode getPreferences(String token) throws Exception {
        MvcResult result = mockMvc.perform(get(PREFERENCES_PATH)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");
    }

    private void updatePreferences(
            String token,
            boolean registrationSuccessful,
            boolean eventUpdated,
            boolean eventCancelled,
            boolean eventReminder) throws Exception {

        String requestBody = """
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

        mockMvc.perform(put(PREFERENCES_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    private EventEntity createReminderEvent(
            UserEntity leader,
            String suffix) {
        OrganizationEntity organization = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Preference Integration " + suffix)
                        .leader(leader)
                        .build()
        );

        return eventRepository.save(
                EventEntity.builder()
                        .title("Preference Reminder " + suffix)
                        .eventDate(LocalDate.now().plusDays(1))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .venue("Integration test venue")
                        .capacity(10)
                        .registrationDeadline(LocalDateTime.now().plusHours(12))
                        .type(EventType.OFFLINE)
                        .organization(organization)
                        .build()
        );
    }

    private long reminderCount(Long userId, Long eventId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(notification ->
                        notification.getType() == NotificationType.EVENT_REMINDER
                                && notification.getEvent().getId().equals(eventId))
                .count();
    }

    private boolean hasNotification(
            String token,
            NotificationType type,
            Long eventId) throws Exception {
        MvcResult result = mockMvc.perform(get(NOTIFICATIONS_PATH)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data").path("content");

        for (JsonNode notification : content) {
            if (type.name().equals(notification.path("type").asText())
                    && (eventId == null || notification.path("eventId").asLong()
                    == eventId)) {
                return true;
            }
        }
        return false;
    }

    private void assertAllEnabled(JsonNode preferences) {
        assertTrue(preferences.path("registrationSuccessful").asBoolean());
        assertTrue(preferences.path("eventUpdated").asBoolean());
        assertTrue(preferences.path("eventCancelled").asBoolean());
        assertTrue(preferences.path("eventReminder").asBoolean());
    }

    private record AuthenticatedUser(UserEntity user, String token) {
    }
}
