package com.collegeeventhub.user.service;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.student.dto.StudentDirectoryResponse;
import com.collegeeventhub.student.service.StudentDirectoryService;
import com.collegeeventhub.user.dto.UserResponse;
import com.collegeeventhub.user.dto.UpdateUserRequest;
import com.collegeeventhub.user.dto.VerifyStudentRequest;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.mapper.UserMapper;
import com.collegeeventhub.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final UserMapper userMapper;
        private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
        private final StudentDirectoryService studentDirectoryService;

    @Override
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse getUserById(Long id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getCurrentUser(String username) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateCurrentUser(String username, UpdateUserRequest request) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setProfileImage(request.getProfileImage());

        UserEntity updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }

        @Override
        public void changePassword(String username, com.collegeeventhub.user.dto.ChangePasswordRequest request) {

                UserEntity user = userRepository.findByUsername(username)
                                .orElseThrow(() ->
                                                new ResourceNotFoundException(
                                                                "User not found with username: " + username));

                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        throw new IllegalArgumentException("Current password is incorrect");
                }

                user.setPassword(passwordEncoder.encode(request.getNewPassword()));

                userRepository.save(user);
        }

    @Override
    @Transactional
    public UserResponse verifyStudent(String username, VerifyStudentRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username: " + username));

        if (user.isProfileVerified()
                && !Objects.equals(user.getRollNumber(), request.getRollNumber())) {
            throw new ConflictException(
                    "A verified student profile cannot change its roll number"
            );
        }

        StudentDirectoryResponse student =
                studentDirectoryService.getStudentByRollNumber(request.getRollNumber());

        if (userRepository.existsByRollNumberAndIdNot(
                student.getRollNumber(),
                user.getId()
        )) {
            throw new ConflictException(
                    "This roll number is already linked to another account"
            );
        }

        user.setRollNumber(student.getRollNumber());
        user.setBranch(student.getBranch());
        user.setAcademicYear(student.getAcademicYear());
        user.setSection(student.getSection());
        user.setCollegeEmail(student.getCollegeEmail());
        user.setProfileVerified(true);

        return userMapper.toResponse(userRepository.save(user));
    }
}
