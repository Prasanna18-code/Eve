package com.collegeeventhub.student.service;

import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.student.dto.StudentDirectoryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StudentDirectoryServiceImpl implements StudentDirectoryService {

    private static final String FIND_ACTIVE_STUDENT_SQL = """
            SELECT roll_number, full_name, branch,
                   academic_year, section, college_email
            FROM student_directory
            WHERE roll_number = ?
            AND active = TRUE
            """;

    private final JdbcTemplate studentDirectoryJdbcTemplate;

    public StudentDirectoryServiceImpl(
            @Qualifier("studentDirectoryJdbcTemplate")
            JdbcTemplate studentDirectoryJdbcTemplate) {
        this.studentDirectoryJdbcTemplate = studentDirectoryJdbcTemplate;
    }

    @Override
    public StudentDirectoryResponse getStudentByRollNumber(String rollNumber) {
        try {
            return studentDirectoryJdbcTemplate.queryForObject(
                    FIND_ACTIVE_STUDENT_SQL,
                    (resultSet, rowNumber) -> StudentDirectoryResponse.builder()
                            .rollNumber(resultSet.getString("roll_number"))
                            .fullName(resultSet.getString("full_name"))
                            .branch(resultSet.getString("branch"))
                            .academicYear(resultSet.getObject(
                                    "academic_year",
                                    Integer.class
                            ))
                            .section(resultSet.getString("section"))
                            .collegeEmail(resultSet.getString("college_email"))
                            .build(),
                    rollNumber
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException(
                    "Active student not found for roll number: " + rollNumber
            );
        }
    }
}
