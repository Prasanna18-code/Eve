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
public class ClubMemberResponse {

    private Long id;

    private Long userId;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private Boolean active;

    private LocalDateTime joinedAt;
}
