package com.collegeeventhub.organization.mapper;

import com.collegeeventhub.organization.dto.OrganizationRequest;
import com.collegeeventhub.organization.dto.OrganizationResponse;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public OrganizationEntity toEntity(OrganizationRequest request) {
        return OrganizationEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .logo(request.getLogo())
                .website(request.getWebsite())
                .build();
    }

    public OrganizationResponse toResponse(OrganizationEntity entity) {
        return OrganizationResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .logo(entity.getLogo())
                .website(entity.getWebsite())
                .active(entity.getActive())
                .leaderId(entity.getLeader().getId())
                .leaderUsername(entity.getLeader().getUsername())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
