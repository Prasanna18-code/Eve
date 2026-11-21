package com.collegeeventhub.registration;

import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.enums.NotificationType;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.notification.repository.NotificationRepository;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.security.JwtService;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class EventRegistrationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void leaderCreatesEventAndVerifiedStudentRegistersThenCancels()
            throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserEntity leader = saveUser("leader-" + suffix, Role.FACULTY);
        UserEntity student = saveUser("student-" + suffix, Role.STUDENT);
        String leaderToken = token(leader);
        String studentToken = token(student);

        mockMvc.perform(post("/api/v1/events/1/registrations"))
                .andExpect(status().isUnauthorized());

        MvcResult organizationResult = mockMvc.perform(post(
                                "/api/v1/organizations")
                        .header("Authorization", bearer(leaderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(organizationRequest(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        long organizationId = dataId(organizationResult);

        MvcResult eventResult = mockMvc.perform(post(
                                "/api/v1/organizations/{organizationId}/events",
                                organizationId
                        )
                        .header("Authorization", bearer(leaderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("UPCOMING"))
                .andReturn();
        long eventId = dataId(eventResult);

        mockMvc.perform(post(
                                "/api/v1/events/{eventId}/registrations",
                                eventId
                        )
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.eventId").value(eventId))
                .andExpect(jsonPath("$.data.username")
                        .value(student.getUsername()));
        assertEquals(1, registrationRepository.countByEventId(eventId));
        assertTrue(notificationRepository.existsByUserIdAndEventIdAndType(
                student.getId(),
                eventId,
                NotificationType.REGISTRATION_SUCCESSFUL
        ));

        mockMvc.perform(get("/api/v1/registrations/me")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete(
                                "/api/v1/events/{eventId}/registrations",
                                eventId
                        )
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/registrations/me")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].eventId").value(eventId));

        assertEquals(0, registrationRepository.countByEventId(eventId));
    }

    private UserEntity saveUser(String username, Role role) {
        return userRepository.saveAndFlush(UserEntity.builder()
                .username(username)
                .email(username + "@example.com")
                .password("test-password")
                .firstName("Test")
                .lastName("User")
                .role(role)
                .active(true)
                .profileVerified(true)
                .build());
    }

    private String token(UserEntity user) {
        return jwtService.generateToken(user.getId(), user.getUsername());
    }

    private String organizationRequest(String suffix) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("name", "Integration Org " + suffix)
                .put("email", "org-" + suffix + "@example.com")
                .put("description", "Organization integration test"));
    }

    private String eventRequest(String suffix) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("title", "Integration Event " + suffix)
                .put("description", "Event integration test")
                .put("eventDate", LocalDate.now().plusDays(3).toString())
                .put("startTime", LocalTime.of(10, 0).toString())
                .put("endTime", LocalTime.of(11, 0).toString())
                .put("venue", "Integration venue")
                .put("capacity", 10)
                .put("registrationDeadline",
                        LocalDateTime.now().plusDays(2).withNano(0).toString())
                .put("type", EventType.OFFLINE.name()));
    }

    private long dataId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.path("data").path("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
