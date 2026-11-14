package com.collegeeventhub.auth.service;

import com.collegeeventhub.auth.dto.AuthResponse;
import com.collegeeventhub.auth.dto.LoginRequest;
import com.collegeeventhub.auth.dto.RegisterRequest;
import com.collegeeventhub.common.exception.DuplicateResourceException;
import com.collegeeventhub.common.exception.InvalidCredentialsException;
import com.collegeeventhub.security.JwtService;
import com.collegeeventhub.user.dto.UserResponse;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.mapper.UserMapper;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        UserEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
