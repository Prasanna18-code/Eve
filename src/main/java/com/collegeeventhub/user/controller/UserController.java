package com.collegeeventhub.user.controller;

import com.collegeeventhub.user.dto.UserResponse;
import com.collegeeventhub.user.service.UserService;
import com.collegeeventhub.security.AuthenticatedUserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.collegeeventhub.user.dto.UpdateUserRequest;
import com.collegeeventhub.user.dto.ChangePasswordRequest;
import com.collegeeventhub.user.dto.VerifyStudentRequest;
import com.collegeeventhub.common.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            Authentication authentication) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                userService.getCurrentUser(username)
        );
    }

        @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                userService.updateCurrentUser(username, request)
        );
    }

    @PutMapping("/me/verify-student")
    public ResponseEntity<ApiResponse<UserResponse>> verifyStudent(
            Authentication authentication,
            @Valid @RequestBody VerifyStudentRequest request) {

        UserResponse response = userService.verifyStudent(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Student profile verified", response)
        );
    }

        @PutMapping("/me/password")
        public ResponseEntity<ApiResponse<Void>> changePassword(
                        Authentication authentication,
                        @Valid @RequestBody ChangePasswordRequest request) {

                String username = authentication.getName();

                userService.changePassword(username, request);

                return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
        }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException(
                    "Authenticated user identity is unavailable");
        }

        return ResponseEntity.ok(
                userService.getUserById(
                        id,
                        principal.getId(),
                        authentication.getAuthorities().stream()
                                .anyMatch(authority ->
                                        "ROLE_ADMIN".equals(authority.getAuthority()))
                )
        );
    }
}
