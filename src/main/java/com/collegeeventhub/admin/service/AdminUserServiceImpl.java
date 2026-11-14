package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId, String adminUsername) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getUsername().equals(adminUsername)) {
            throw new IllegalStateException(
                    "You cannot deactivate your own account");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    private AdminUserResponse toResponse(UserEntity user) {

        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
