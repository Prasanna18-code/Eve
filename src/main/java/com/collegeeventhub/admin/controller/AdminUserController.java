package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.admin.service.AdminUserService;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsers() {

        List<AdminUserResponse> users = adminUserService.getAllUsers();

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
}
