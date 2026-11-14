package com.collegeeventhub.auth.service;

import com.collegeeventhub.auth.dto.AuthResponse;
import com.collegeeventhub.auth.dto.LoginRequest;
import com.collegeeventhub.auth.dto.RegisterRequest;
import com.collegeeventhub.user.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
