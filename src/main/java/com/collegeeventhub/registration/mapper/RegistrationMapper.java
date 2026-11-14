package com.collegeeventhub.registration.mapper;

import com.collegeeventhub.registration.dto.RegistrationResponse;
import com.collegeeventhub.registration.entity.RegistrationEntity;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {

    public RegistrationResponse toResponse(RegistrationEntity entity) {

        return RegistrationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .username(entity.getUser().getUsername())
                .eventId(entity.getEvent().getId())
                .eventTitle(entity.getEvent().getTitle())
                .registeredAt(entity.getRegisteredAt())
                .build();
    }
}
