package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.dto.AdminUserPageResponse;
import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.admin.dto.AdminUserRoleRequest;
import com.collegeeventhub.admin.service.AdminUserService;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.BadRequestException;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserPageResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active) {

        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }
        if (size < 1 || size > 50) {
            throw new BadRequestException("Page size must be between 1 and 50");
        }

        AdminUserPageResponse users =
                adminUserService.getAllUsers(page, size, role, active);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Users fetched successfully", users)
        );
    }

    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @PathVariable Long userId) {

        adminUserService.activateUser(userId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User activated successfully", null)
        );
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable Long userId,
            Authentication authentication) {
        if (!(authentication.getPrincipal()
                instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException(
                    "Authenticated user identity is unavailable");
        }

        adminUserService.deactivateUser(
                userId,
                principal.getId()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User deactivated successfully", null)
        );
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleRequest request,
            Authentication authentication) {
        if (!(authentication.getPrincipal()
                instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException(
                    "Authenticated user identity is unavailable");
        }

        AdminUserResponse user = adminUserService.updateUserRole(
                userId,
                request.getRole(),
                principal.getId()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User role updated successfully", user)
        );
    }
}
