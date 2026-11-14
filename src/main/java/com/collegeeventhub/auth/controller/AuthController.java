package com.collegeeventhub.auth.controller;

import com.collegeeventhub.auth.dto.AuthResponse;
import com.collegeeventhub.auth.dto.LoginRequest;
import com.collegeeventhub.auth.dto.RegisterRequest;
import com.collegeeventhub.auth.service.AuthService;
import com.collegeeventhub.common.constants.ApiConstants;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_VERSION + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response));
    }
}
