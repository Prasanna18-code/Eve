package com.collegeeventhub.admin.service;

import com.collegeeventhub.admin.dto.AdminUserResponse;

import java.util.List;

public interface AdminUserService {

    List<AdminUserResponse> getAllUsers();

    void activateUser(Long userId);

    void deactivateUser(Long userId, Long adminUserId);
}
