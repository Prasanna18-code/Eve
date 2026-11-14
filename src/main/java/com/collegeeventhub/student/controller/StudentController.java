package com.collegeeventhub.student.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.student.dto.StudentDirectoryResponse;
import com.collegeeventhub.student.service.StudentDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentDirectoryService studentDirectoryService;

    @GetMapping("/{rollNumber}")
    public ResponseEntity<ApiResponse<StudentDirectoryResponse>> getStudentByRollNumber(
            @PathVariable String rollNumber) {

        StudentDirectoryResponse student =
                studentDirectoryService.getStudentByRollNumber(rollNumber);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Student found", student)
        );
    }
}
