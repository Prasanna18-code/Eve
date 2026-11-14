package com.collegeeventhub.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubResponse {

    private Long id;

    private String name;

    private String description;

    private String email;

    private String phoneNumber;

    private String logo;

    private Boolean active;

    private Long organizationId;

    private String organizationName;

    private Long leaderId;

    private String leaderUsername;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
