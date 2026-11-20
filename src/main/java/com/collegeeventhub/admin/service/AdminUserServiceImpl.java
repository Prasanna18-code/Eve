package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.admin.dto.AdminUserPageResponse;
import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.BadRequestException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminUserPageResponse getAllUsers(
            int page,
            int size,
            Role role,
            Boolean active) {

        Page<UserEntity> users = userRepository.findAllByFilters(
                role,
                active,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                )
        );

        return AdminUserPageResponse.builder()
                .content(users.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .last(users.isLast())
                .build();
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
    public void deactivateUser(Long userId, Long adminUserId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getId().equals(adminUserId)) {
            throw new BadRequestException(
                    "You cannot deactivate your own account");
        }

        if (Boolean.TRUE.equals(user.getActive()) && user.getRole() == Role.ADMIN) {
            ensureAnotherActiveAdminRemains();
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRole(
            Long userId,
            Role role,
            Long adminUserId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getId().equals(adminUserId) && user.getRole() != role) {
            throw new BadRequestException(
                    "You cannot change your own role");
        }

        if (Boolean.TRUE.equals(user.getActive())
                && user.getRole() == Role.ADMIN
                && role != Role.ADMIN) {
            ensureAnotherActiveAdminRemains();
        }

        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    private void ensureAnotherActiveAdminRemains() {
        List<UserEntity> activeAdmins =
                userRepository.findActiveUsersByRoleForUpdate(Role.ADMIN);
        if (activeAdmins.size() <= 1) {
            throw new BadRequestException(
                    "At least one active administrator must remain");
        }
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
