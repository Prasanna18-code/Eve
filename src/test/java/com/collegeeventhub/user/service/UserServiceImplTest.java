package com.collegeeventhub.user.service;

import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.student.dto.StudentDirectoryResponse;
import com.collegeeventhub.student.service.StudentDirectoryService;
import com.collegeeventhub.user.dto.UserResponse;
import com.collegeeventhub.user.dto.VerifyStudentRequest;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.mapper.UserMapper;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StudentDirectoryService studentDirectoryService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity user;
    private StudentDirectoryResponse directoryStudent;
    private VerifyStudentRequest request;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(42L)
                .username("student42")
                .build();

        directoryStudent = StudentDirectoryResponse.builder()
                .rollNumber("1MS23CS132")
                .fullName("Student Name")
                .branch("CSE")
                .academicYear(3)
                .section("A")
                .collegeEmail("student132@example.edu")
                .build();

        request = new VerifyStudentRequest();
        request.setRollNumber("1MS23CS132");
    }

    @Test
    void verifyStudentCopiesDirectoryDetailsAndSavesVerifiedProfile() {
        UserResponse response = UserResponse.builder()
                .rollNumber(directoryStudent.getRollNumber())
                .profileVerified(true)
                .build();

        when(userRepository.findByUsername("student42"))
                .thenReturn(Optional.of(user));
        when(studentDirectoryService.getStudentByRollNumber("1MS23CS132"))
                .thenReturn(directoryStudent);
        when(userRepository.existsByRollNumberAndIdNot("1MS23CS132", 42L))
                .thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.verifyStudent("student42", request);

        assertEquals("1MS23CS132", user.getRollNumber());
        assertEquals("CSE", user.getBranch());
        assertEquals(3, user.getAcademicYear());
        assertEquals("A", user.getSection());
        assertEquals("student132@example.edu", user.getCollegeEmail());
        assertTrue(user.isProfileVerified());
        assertTrue(result.isProfileVerified());
        verify(userRepository).save(same(user));
    }

    @Test
    void verifyStudentPropagatesUnknownRollNumberWithoutSaving() {
        when(userRepository.findByUsername("student42"))
                .thenReturn(Optional.of(user));
        when(studentDirectoryService.getStudentByRollNumber("1MS23CS132"))
                .thenThrow(new ResourceNotFoundException("Active student not found"));

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.verifyStudent("student42", request)
        );

        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(userMapper);
    }

    @Test
    void verifyStudentRejectsRollNumberLinkedToAnotherUser() {
        when(userRepository.findByUsername("student42"))
                .thenReturn(Optional.of(user));
        when(studentDirectoryService.getStudentByRollNumber("1MS23CS132"))
                .thenReturn(directoryStudent);
        when(userRepository.existsByRollNumberAndIdNot("1MS23CS132", 42L))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> userService.verifyStudent("student42", request)
        );

        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(userMapper);
    }

    @Test
    void verifyStudentRejectsChangingAlreadyVerifiedRollNumber() {
        user.setProfileVerified(true);
        user.setRollNumber("1MS22CS001");

        when(userRepository.findByUsername("student42"))
                .thenReturn(Optional.of(user));

        assertThrows(
                ConflictException.class,
                () -> userService.verifyStudent("student42", request)
        );

        verifyNoInteractions(studentDirectoryService);
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(userMapper);
        assertFalse(user.getRollNumber().equals(request.getRollNumber()));
    }
}
