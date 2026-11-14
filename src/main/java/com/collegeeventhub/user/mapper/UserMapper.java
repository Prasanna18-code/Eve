package com.collegeeventhub.user.mapper;

import com.collegeeventhub.auth.dto.RegisterRequest;
import com.collegeeventhub.user.dto.UserResponse;
import com.collegeeventhub.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(RegisterRequest request) {
        return UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .build();
    }

    public UserResponse toResponse(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phoneNumber(entity.getPhoneNumber())
                .profileImage(entity.getProfileImage())
                .rollNumber(entity.getRollNumber())
                .branch(entity.getBranch())
                .academicYear(entity.getAcademicYear())
                .section(entity.getSection())
                .collegeEmail(entity.getCollegeEmail())
                .profileVerified(entity.isProfileVerified())
                .role(entity.getRole())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
