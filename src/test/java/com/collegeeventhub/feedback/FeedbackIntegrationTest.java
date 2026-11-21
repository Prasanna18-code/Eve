package com.collegeeventhub.feedback;

import com.collegeeventhub.common.enums.EventStatus;
import com.collegeeventhub.common.enums.EventType;
import com.collegeeventhub.common.enums.RegistrationStatus;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.event.repository.EventRepository;
import com.collegeeventhub.feedback.dto.FeedbackRequest;
import com.collegeeventhub.feedback.repository.FeedbackRepository;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import com.collegeeventhub.registration.repository.RegistrationRepository;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class FeedbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void registeredStudentCanSubmitEditAndRetrieveFeedbackWhileOrganizerAndAdminCanReview()
            throws Exception {
        TestContext context = createContext(true, true, EventStatus.COMPLETED);

        MvcResult created = mockMvc.perform(post(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(5, "  Great event!  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("Great event!"))
                .andReturn();
        Long feedbackId = objectMapper.readTree(
                created.getResponse().getContentAsString()
        ).path("data").path("id").asLong();

        mockMvc.perform(post(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(4, "Duplicate")))
                .andExpect(status().isConflict());
        assertEquals(1, feedbackRepository.count());

        mockMvc.perform(put(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(4, "Updated feedback")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(feedbackId))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.comment").value("Updated feedback"));

        registrationRepository.saveAndFlush(RegistrationEntity.builder()
                .user(context.otherStudent())
                .event(context.event())
                .status(RegistrationStatus.REGISTERED)
                .build());
        mockMvc.perform(put(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.otherStudent())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(1, "Not my feedback")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                                "/api/v1/events/{eventId}/feedback/me",
                                context.event().getId()
                        )
                        .with(user(principal(context.student()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(feedbackId));

        mockMvc.perform(get(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.leader()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].username")
                        .value(context.student().getUsername()));

        mockMvc.perform(get(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.admin()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.otherStudent()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void feedbackRequiresVerifiedRegistrationAndEventCompletion()
            throws Exception {
        TestContext unverified = createContext(
                false,
                true,
                EventStatus.COMPLETED
        );
        mockMvc.perform(post(
                                "/api/v1/events/{eventId}/feedback",
                                unverified.event().getId()
                        )
                        .with(user(principal(unverified.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(4, null)))
                .andExpect(status().isForbidden());

        TestContext unregistered = createContext(
                true,
                false,
                EventStatus.COMPLETED
        );
        mockMvc.perform(post(
                                "/api/v1/events/{eventId}/feedback",
                                unregistered.event().getId()
                        )
                        .with(user(principal(unregistered.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(4, null)))
                .andExpect(status().isForbidden());

        TestContext upcoming = createContext(
                true,
                true,
                EventStatus.UPCOMING
        );
        mockMvc.perform(post(
                                "/api/v1/events/{eventId}/feedback",
                                upcoming.event().getId()
                        )
                        .with(user(principal(upcoming.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(4, null)))
                .andExpect(status().isConflict());

        TestContext cancelled = createContext(
                true,
                true,
                EventStatus.CANCELLED
        );
        mockMvc.perform(post(
                                "/api/v1/events/{eventId}/feedback",
                                cancelled.event().getId()
                        )
                        .with(user(principal(cancelled.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(4, null)))
                .andExpect(status().isConflict());
    }

    @Test
    void ratingOutsideTheSupportedRangeIsRejected() throws Exception {
        TestContext context = createContext(true, true, EventStatus.COMPLETED);

        mockMvc.perform(post(
                                "/api/v1/events/{eventId}/feedback",
                                context.event().getId()
                        )
                        .with(user(principal(context.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(6, "Invalid rating")))
                .andExpect(status().isBadRequest());
    }

    private TestContext createContext(
            boolean verified,
            boolean registered,
            EventStatus status) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        UserEntity leader = saveUser("leader", suffix, Role.STUDENT, true);
        UserEntity student = saveUser("student", suffix, Role.STUDENT, verified);
        UserEntity admin = saveUser("admin", suffix, Role.ADMIN, true);
        UserEntity otherStudent = saveUser(
                "other",
                suffix,
                Role.STUDENT,
                true
        );
        OrganizationEntity organization = organizationRepository.saveAndFlush(
                OrganizationEntity.builder()
                        .name("Feedback Org " + suffix)
                        .leader(leader)
                        .build()
        );
        EventEntity event = eventRepository.saveAndFlush(
                EventEntity.builder()
                        .title("Feedback Event " + suffix)
                        .eventDate(status == EventStatus.UPCOMING
                                ? LocalDate.now().plusDays(2)
                                : LocalDate.now().minusDays(1))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .venue("Test venue")
                        .capacity(20)
                        .registrationDeadline(LocalDateTime.now().plusDays(1))
                        .type(EventType.OFFLINE)
                        .status(status)
                        .organization(organization)
                        .build()
        );
        if (registered) {
            registrationRepository.saveAndFlush(
                    RegistrationEntity.builder()
                            .user(student)
                            .event(event)
                            .status(RegistrationStatus.REGISTERED)
                            .build()
            );
        }
        return new TestContext(leader, student, admin, otherStudent, event);
    }

    private UserEntity saveUser(
            String username,
            String suffix,
            Role role,
            boolean verified) {
        return userRepository.saveAndFlush(UserEntity.builder()
                .username("feedback-" + username + "-" + suffix)
                .email("feedback-" + username + "-" + suffix + "@example.com")
                .password("test-password")
                .firstName("Test")
                .lastName(username)
                .role(role)
                .active(true)
                .profileVerified(verified)
                .build());
    }

    private AuthenticatedUserPrincipal principal(UserEntity user) {
        return new AuthenticatedUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole().name(),
                true
        );
    }

    private String jsonRequest(Integer rating, String comment)
            throws Exception {
        return objectMapper.writeValueAsString(
                new FeedbackRequest(rating, comment)
        );
    }

    private record TestContext(
            UserEntity leader,
            UserEntity student,
            UserEntity admin,
            UserEntity otherStudent,
            EventEntity event) {
    }
}
