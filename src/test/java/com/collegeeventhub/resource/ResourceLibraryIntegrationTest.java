package com.collegeeventhub.resource;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ResourceLibraryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void adminCanPublishResourcesAndVerifiedStudentsCanSearchAndReadThem()
            throws Exception {
        TestContext context = createContext();
        MvcResult created = mockMvc.perform(post("/api/v1/resources")
                        .with(user(principal(context.admin())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                context.organization().getId(),
                                "Spring Boot Guide",
                                "A practical guide to Spring Boot",
                                "https://example.com/spring-guide",
                                "Programming",
                                "[\"spring\", \"java\"]"
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.organizationId")
                        .value(context.organization().getId()))
                .andReturn();
        Long resourceId = objectMapper.readTree(
                created.getResponse().getContentAsString()
        ).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/resources")
                        .with(user(principal(context.student()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/resources")
                        .with(user(principal(context.student())))
                        .param("q", "spring")
                        .param("category", "programming")
                        .param("tag", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(resourceId))
                .andExpect(jsonPath("$.data.content[0].tags.length()").value(2));

        mockMvc.perform(get("/api/v1/resources/{resourceId}", resourceId)
                        .with(user(principal(context.student()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring Boot Guide"))
                .andExpect(jsonPath("$.data.url")
                        .value("https://example.com/spring-guide"));
    }

    @Test
    void organizationLeadersCanManageOnlyTheirOwnResources()
            throws Exception {
        TestContext context = createContext();
        MvcResult created = mockMvc.perform(post("/api/v1/resources")
                        .with(user(principal(context.leader())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                context.organization().getId(),
                                "Algorithms",
                                "Algorithms reference",
                                "https://example.com/algorithms",
                                "Computer Science",
                                "[\"dsa\"]"
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        Long resourceId = objectMapper.readTree(
                created.getResponse().getContentAsString()
        ).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/resources/{resourceId}", resourceId)
                        .with(user(principal(context.leader())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest(
                                "Advanced Algorithms",
                                "Updated reference",
                                "https://example.com/advanced-algorithms",
                                "Computer Science",
                                "[\"dsa\", \"graphs\"]"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title")
                        .value("Advanced Algorithms"));

        mockMvc.perform(put("/api/v1/resources/{resourceId}", resourceId)
                        .with(user(principal(context.otherLeader())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest(
                                "Unauthorized change",
                                "No access",
                                "https://example.com/not-allowed",
                                "Computer Science",
                                "[]"
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/resources/manage")
                        .with(user(principal(context.leader()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(resourceId));

        mockMvc.perform(delete("/api/v1/resources/{resourceId}", resourceId)
                        .with(user(principal(context.leader()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/resources/{resourceId}", resourceId)
                        .with(user(principal(context.student()))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/resources")
                        .with(user(principal(context.student())))
                        .param("q", "advanced algorithms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void unverifiedStudentsAndNonManagersCannotManageResources()
            throws Exception {
        TestContext context = createContext();

        mockMvc.perform(get("/api/v1/resources")
                        .with(user(principal(context.unverifiedStudent()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/resources")
                        .with(user(principal(context.student())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                context.organization().getId(),
                                "Unauthorized",
                                "Not allowed",
                                "https://example.com/not-allowed",
                                "General",
                                "[]"
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/resources")
                        .with(user(principal(context.admin())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(
                                context.organization().getId(),
                                "Invalid link",
                                "Malformed URL",
                                "javascript:alert(1)",
                                "General",
                                "[]"
                        )))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/resources/manage")
                        .with(user(principal(context.student()))))
                .andExpect(status().isForbidden());
    }

    private TestContext createContext() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8);
        UserEntity admin = saveUser("admin", suffix, Role.ADMIN, true);
        UserEntity student = saveUser("student", suffix, Role.STUDENT, true);
        UserEntity unverifiedStudent = saveUser(
                "unverified",
                suffix,
                Role.STUDENT,
                false
        );
        UserEntity leader = saveUser("leader", suffix, Role.FACULTY, true);
        UserEntity otherLeader = saveUser(
                "other-leader",
                suffix,
                Role.FACULTY,
                true
        );
        OrganizationEntity organization = organizationRepository.saveAndFlush(
                OrganizationEntity.builder()
                        .name("Resource Org " + suffix)
                        .leader(leader)
                        .build()
        );
        return new TestContext(
                admin,
                student,
                unverifiedStudent,
                leader,
                otherLeader,
                organization
        );
    }

    private UserEntity saveUser(
            String username,
            String suffix,
            Role role,
            boolean verified) {
        return userRepository.saveAndFlush(UserEntity.builder()
                .username("resource-" + username + "-" + suffix)
                .email("resource-" + username + "-" + suffix + "@example.com")
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

    private String createRequest(
            Long organizationId,
            String title,
            String description,
            String url,
            String category,
            String tags) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("organizationId", organizationId)
                .put("title", title)
                .put("description", description)
                .put("url", url)
                .put("category", category)
                .set("tags", objectMapper.readTree(tags)));
    }

    private String updateRequest(
            String title,
            String description,
            String url,
            String category,
            String tags) throws Exception {
        JsonNode request = objectMapper.createObjectNode()
                .put("title", title)
                .put("description", description)
                .put("url", url)
                .put("category", category)
                .set("tags", objectMapper.readTree(tags));
        return objectMapper.writeValueAsString(request);
    }

    private record TestContext(
            UserEntity admin,
            UserEntity student,
            UserEntity unverifiedStudent,
            UserEntity leader,
            UserEntity otherLeader,
            OrganizationEntity organization) {
    }
}
