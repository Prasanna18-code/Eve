package com.collegeeventhub.placement.dto;

import com.collegeeventhub.placement.entity.PlacementApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementApplicantResponse {

    private Long applicationId;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String collegeEmail;
    private String rollNumber;
    private String branch;
    private Integer academicYear;
    private PlacementApplicationStatus status;
    private LocalDateTime appliedAt;
}
