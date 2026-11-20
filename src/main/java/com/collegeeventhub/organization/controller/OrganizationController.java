package com.collegeeventhub.organization.controller;

import com.collegeeventhub.common.constants.ApiConstants;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.organization.dto.OrganizationRequest;
import com.collegeeventhub.organization.dto.OrganizationResponse;
import com.collegeeventhub.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(ApiConstants.API_VERSION + "/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            Authentication authentication,
            @Valid @RequestBody OrganizationRequest request) {

        String username = authentication.getName();

        OrganizationResponse response =
                organizationService.createOrganization(username, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Organization created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrganizationResponse>>>
    getAllOrganizations(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) Boolean active) {
        Pagination.validate(page, size);
        PageResponse<OrganizationResponse> organizations =
                organizationService.getAllOrganizations(page, size, active);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Organizations retrieved successfully", organizations)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationById(
            @PathVariable Long id) {

        OrganizationResponse response =
                organizationService.getOrganizationById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Organization retrieved successfully", response)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody OrganizationRequest request) {

        String username = authentication.getName();

        OrganizationResponse response =
                organizationService.updateOrganization(id, username, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Organization updated successfully", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            @PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();

        organizationService.deleteOrganization(id, username);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Organization deleted successfully", null)
        );
    }
}
