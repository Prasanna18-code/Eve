package com.collegeeventhub.student.service;

import com.collegeeventhub.student.dto.StudentDirectoryResponse;

public interface StudentDirectoryService {

    StudentDirectoryResponse getStudentByRollNumber(String rollNumber);
}
