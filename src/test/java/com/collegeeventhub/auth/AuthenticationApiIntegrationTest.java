package com.collegeeventhub.auth;

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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AuthenticationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationLoginAndJwtProtectedProfileFlowPersistsChanges()
            throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String firstUsername = "jwt-user-" + suffix;
        String secondUsername = "jwt-other-" + suffix;
        String password = "secure-test-password";

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        long firstUserId = register(
                firstUsername,
                firstUsername + "@example.com",
                password
        );
        long secondUserId = register(
                secondUsername,
                secondUsername + "@example.com",
                password
        );
        assertNotEquals(firstUserId, secondUserId);

        String firstToken = login(firstUsername, password);
        String secondToken = login(secondUsername, password);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(firstUsername, "wrong-password")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstUserId))
                .andExpect(jsonPath("$.username").value(firstUsername));

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", bearer(firstToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileUpdateRequest(
                                "Updated",
                                "Profile",
                                "5551234567"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("5551234567"));

        mockMvc.perform(get("/api/v1/users/{id}", secondUserId)
                        .header("Authorization", bearer(firstToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users/{id}", firstUserId)
                        .header("Authorization", bearer(secondToken)))
                .andExpect(status().isForbidden());
    }

    private long register(String username, String email, String password)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(username, email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value(username))
                .andReturn();
        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).path("data").path("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("Bearer"))
                .andReturn();
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.path("data").path("token").asText();
    }

    private String registerRequest(
            String username,
            String email,
            String password) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("username", username)
                .put("email", email)
                .put("password", password)
                .put("firstName", "Test")
                .put("lastName", "User"));
    }

    private String loginRequest(String username, String password)
            throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password));
    }

    private String profileUpdateRequest(
            String firstName,
            String lastName,
            String phoneNumber) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("firstName", firstName)
                .put("lastName", lastName)
                .put("phoneNumber", phoneNumber));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
