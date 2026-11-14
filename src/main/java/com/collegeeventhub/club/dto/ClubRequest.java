package com.collegeeventhub.club.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubRequest {

    @NotBlank
    private String name;

    private String description;

    @Email
    private String email;

    private String phoneNumber;

    private String logo;
}
