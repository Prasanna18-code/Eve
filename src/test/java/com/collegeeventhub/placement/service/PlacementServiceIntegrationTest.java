package com.collegeeventhub.placement.service;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.placement.dto.PlacementApplicantResponse;
import com.collegeeventhub.placement.dto.PlacementApplicationResponse;
import com.collegeeventhub.placement.dto.PlacementRequest;
import com.collegeeventhub.placement.dto.PlacementResponse;
import com.collegeeventhub.placement.entity.PlacementApplicationEntity;
import com.collegeeventhub.placement.entity.PlacementEntity;
import com.collegeeventhub.placement.entity.PlacementStatus;
import com.collegeeventhub.placement.entity.PlacementType;
import com.collegeeventhub.placement.repository.PlacementApplicationRepository;
import com.collegeeventhub.placement.repository.PlacementRepository;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class PlacementServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private PlacementService placementService;

    @Autowired
    private PlacementRepository placementRepository;

    @Autowired
    private PlacementApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void placementWorkflowIsAvailableThroughAuthenticatedApiEndpoints()
            throws Exception {
        UserEntity admin = userRepository.saveAndFlush(testUser(Role.ADMIN, true));
        UserEntity student = userRepository.saveAndFlush(
                testUser(Role.STUDENT, true)
        );
        PlacementRequest createRequest =
                request(LocalDateTime.now().plusDays(10));

        MvcResult createResult = mockMvc.perform(
                        post("/api/v1/admin/placements")
                                .with(user(principal(admin)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        createRequest
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        Long placementId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()
        ).path("data").path("id").asLong();

        PlacementRequest updateRequest =
                request(LocalDateTime.now().plusDays(12));
        updateRequest.setTitle("Updated Software Engineer");
        mockMvc.perform(put("/api/v1/admin/placements/{placementId}", placementId)
                        .with(user(principal(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title")
                        .value("Updated Software Engineer"));

        mockMvc.perform(put(
                        "/api/v1/admin/placements/{placementId}/publish",
                        placementId
                ).with(user(principal(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/placements")
                        .with(user(principal(student))))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.content[*].id",
                        org.hamcrest.Matchers.hasItem(placementId.intValue())
                ));

        mockMvc.perform(post(
                        "/api/v1/placements/{placementId}/applications",
                        placementId
                ).with(user(principal(student))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.placementId").value(placementId));

        mockMvc.perform(get("/api/v1/placements/applications/me")
                        .with(user(principal(student))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get(
                        "/api/v1/admin/placements/{placementId}/applications",
                        placementId
                ).with(user(principal(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].userId")
                        .value(student.getId()));

        mockMvc.perform(put(
                        "/api/v1/admin/placements/{placementId}/close",
                        placementId
                ).with(user(principal(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void adminCanPublishAndVerifiedStudentCanApplyOnlyOnce() {
        UserEntity admin = userRepository.saveAndFlush(testUser(Role.ADMIN, true));
        UserEntity student = userRepository.saveAndFlush(
                testUser(Role.STUDENT, true)
        );
        PlacementResponse draft = placementService.createPlacement(
                request(LocalDateTime.now().plusDays(10)),
                admin.getId()
        );

        assertEquals(PlacementStatus.DRAFT, draft.getStatus());
        assertEquals(0, placementService.getPublishedPlacements(
                0,
                10,
                null
        ).getTotalElements());

        PlacementResponse published = placementService.publishPlacement(
                draft.getId(),
                admin.getId()
        );
        assertEquals(PlacementStatus.PUBLISHED, published.getStatus());

        PlacementApplicationResponse application = placementService.apply(
                draft.getId(),
                student.getUsername()
        );
        assertEquals(draft.getId(), application.getPlacementId());
        assertEquals("Example Co.", application.getCompanyName());

        assertThrows(
                ConflictException.class,
                () -> placementService.apply(
                        draft.getId(),
                        student.getUsername()
                )
        );

        PageResponse<PlacementApplicantResponse> applicants =
                placementService.getApplicants(draft.getId(), 0, 10);
        assertEquals(1, applicants.getTotalElements());
        assertEquals(student.getId(), applicants.getContent().get(0).getUserId());

        PageResponse<PlacementApplicationResponse> mine =
                placementService.getMyApplications(
                        0,
                        10,
                        student.getUsername()
                );
        assertEquals(1, mine.getTotalElements());
    }

    @Test
    void unverifiedNonStudentAndClosedPlacementCannotReceiveApplications() {
        UserEntity admin = userRepository.saveAndFlush(testUser(Role.ADMIN, true));
        UserEntity unverified = userRepository.saveAndFlush(
                testUser(Role.STUDENT, false)
        );
        UserEntity faculty = userRepository.saveAndFlush(
                testUser(Role.FACULTY, true)
        );
        UserEntity verifiedStudent = userRepository.saveAndFlush(
                testUser(Role.STUDENT, true)
        );
        PlacementResponse draft = placementService.createPlacement(
                request(LocalDateTime.now().plusDays(10)),
                admin.getId()
        );
        placementService.publishPlacement(draft.getId(), admin.getId());

        assertThrows(
                AccessDeniedException.class,
                () -> placementService.apply(
                        draft.getId(),
                        unverified.getUsername()
                )
        );
        assertThrows(
                AccessDeniedException.class,
                () -> placementService.apply(
                        draft.getId(),
                        faculty.getUsername()
                )
        );

        placementService.closePlacement(draft.getId(), admin.getId());
        assertThrows(
                com.collegeeventhub.common.exception.ResourceNotFoundException.class,
                () -> placementService.apply(
                        draft.getId(),
                        verifiedStudent.getUsername()
                )
        );
    }

    @Test
    void applicationDeadlineIsEnforced() {
        UserEntity admin = userRepository.saveAndFlush(testUser(Role.ADMIN, true));
        UserEntity student = userRepository.saveAndFlush(
                testUser(Role.STUDENT, true)
        );
        PlacementResponse response = placementService.createPlacement(
                request(LocalDateTime.now().plusDays(10)),
                admin.getId()
        );
        PlacementEntity placement = placementRepository.findById(response.getId())
                .orElseThrow();
        placement.setApplicationDeadline(LocalDateTime.now().minusMinutes(1));
        placement.setStatus(PlacementStatus.PUBLISHED);
        placementRepository.saveAndFlush(placement);

        long applicationCountBefore = applicationRepository.count();
        assertThrows(
                ConflictException.class,
                () -> placementService.apply(
                        placement.getId(),
                        student.getUsername()
                )
        );
        assertEquals(applicationCountBefore, applicationRepository.count());
    }

    private UserEntity testUser(Role role, boolean profileVerified) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return UserEntity.builder()
                .username("placement-" + suffix)
                .email("placement-" + suffix + "@example.com")
                .password("test-password")
                .firstName("Placement")
                .lastName("User")
                .role(role)
                .active(true)
                .profileVerified(profileVerified)
                .rollNumber("R" + suffix)
                .branch("CSE")
                .academicYear(3)
                .collegeEmail("placement-" + suffix + "@college.edu")
                .build();
    }

    private PlacementRequest request(LocalDateTime deadline) {
        return PlacementRequest.builder()
                .companyName("Example Co.")
                .title("Software Engineer")
                .description("Graduate engineering opportunity.")
                .location("Remote")
                .type(PlacementType.FULL_TIME)
                .compensation("$100,000")
                .eligibilityCriteria("Final-year students")
                .applicationDeadline(deadline)
                .build();
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
}
