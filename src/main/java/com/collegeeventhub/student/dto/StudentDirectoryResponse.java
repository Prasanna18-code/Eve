package com.collegeeventhub.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDirectoryResponse {

    private String rollNumber;

    private String fullName;

    private String branch;

    private Integer academicYear;

    private String section;

    private String collegeEmail;
}
