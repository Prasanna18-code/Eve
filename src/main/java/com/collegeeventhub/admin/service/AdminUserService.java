package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminUserPageResponse;
import com.collegeeventhub.admin.dto.AdminUserResponse;
import com.collegeeventhub.common.enums.Role;

public interface AdminUserService {

    AdminUserPageResponse getAllUsers(
            int page,
            int size,
            Role role,
            Boolean active
    );

    void activateUser(Long userId, Long adminUserId);

    void deactivateUser(Long userId, Long adminUserId);

    AdminUserResponse updateUserRole(
            Long userId,
            Role role,
            Long adminUserId
    );
}
