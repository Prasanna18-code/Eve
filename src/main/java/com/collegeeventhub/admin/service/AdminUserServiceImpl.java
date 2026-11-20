package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.admin.dto.AdminUserPageResponse;
import com.collegeeventhub.admin.audit.AdminAuditAction;
import com.collegeeventhub.admin.audit.AdminAuditLogService;
import com.collegeeventhub.admin.audit.AdminAuditTargetType;
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
    private final AdminAuditLogService adminAuditLogService;

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
    public void activateUser(Long userId, Long adminUserId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        boolean wasActive = Boolean.TRUE.equals(user.getActive());
        user.setActive(true);
        userRepository.save(user);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.USER_ACTIVATED,
                AdminAuditTargetType.USER,
                userId,
                "active changed from " + wasActive + " to true"
        );
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

        boolean wasActive = Boolean.TRUE.equals(user.getActive());
        user.setActive(false);
        userRepository.save(user);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.USER_DEACTIVATED,
                AdminAuditTargetType.USER,
                userId,
                "active changed from " + wasActive + " to false"
        );
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

        Role previousRole = user.getRole();
        user.setRole(role);
        userRepository.save(user);
        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.USER_ROLE_CHANGED,
                AdminAuditTargetType.USER,
                userId,
                "role changed from " + previousRole + " to " + role
        );
        return toResponse(user);
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
