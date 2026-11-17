package com.collegeeventhub.user.service;

import com.collegeeventhub.user.dto.UserResponse;
import com.collegeeventhub.user.dto.UpdateUserRequest;
import com.collegeeventhub.user.dto.VerifyStudentRequest;

import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id, Long requesterUserId, boolean requesterIsAdmin);

    UserResponse getCurrentUser(String username);

        UserResponse updateCurrentUser(
            String username,
            UpdateUserRequest request
        );

    void changePassword(
            String username,
            com.collegeeventhub.user.dto.ChangePasswordRequest request
    );

    UserResponse verifyStudent(
            String username,
            VerifyStudentRequest request
    );

}
