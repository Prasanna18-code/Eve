package com.collegeeventhub.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

    private Long id;

    private Long userId;
    private String username;

    private Long eventId;
    private String eventTitle;

    private LocalDateTime registeredAt;
}
