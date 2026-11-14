package com.collegeeventhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class StudentDirectoryConnectionTest {

    @Autowired
    @Qualifier("studentDirectoryJdbcTemplate")
    private JdbcTemplate studentDirectoryJdbcTemplate;

    @Test
    void connectsAndReadsStudentDirectoryCount() {
        Long studentCount = studentDirectoryJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_directory",
                Long.class
        );

        assertNotNull(studentCount);
        assertTrue(studentCount >= 0);
    }
}
