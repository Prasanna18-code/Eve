package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.dto.AdminOrganizationResponse;
import com.collegeeventhub.admin.service.AdminOrganizationService;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/organizations")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminOrganizationResponse>>>
    getAllOrganizations(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) Boolean active) {
        Pagination.validate(page, size);
        PageResponse<AdminOrganizationResponse> organizations =
                adminOrganizationService.getAllOrganizations(page, size, active);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Organizations fetched successfully",
                        organizations
                )
        );
    }

    @PutMapping("/{organizationId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateOrganization(
            @PathVariable Long organizationId) {

        adminOrganizationService.activateOrganization(organizationId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Organization activated successfully",
                        null
                )
        );
    }

    @PutMapping("/{organizationId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateOrganization(
            @PathVariable Long organizationId) {

        adminOrganizationService.deactivateOrganization(organizationId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Organization deactivated successfully",
                        null
                )
        );
    }
}
