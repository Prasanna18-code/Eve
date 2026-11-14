package com.collegeeventhub.club.mapper;

import com.collegeeventhub.club.dto.ClubRequest;
import com.collegeeventhub.club.dto.ClubResponse;
import com.collegeeventhub.club.entity.ClubEntity;
import org.springframework.stereotype.Component;

@Component
public class ClubMapper {

    public ClubEntity toEntity(ClubRequest request) {
        return ClubEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .logo(request.getLogo())
                .build();
    }

    public ClubResponse toResponse(ClubEntity club) {
        return ClubResponse.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .email(club.getEmail())
                .phoneNumber(club.getPhoneNumber())
                .logo(club.getLogo())
                .active(club.getActive())
                .organizationId(club.getOrganization().getId())
                .organizationName(club.getOrganization().getName())
                .leaderId(club.getLeader().getId())
                .leaderUsername(club.getLeader().getUsername())
                .createdAt(club.getCreatedAt())
                .updatedAt(club.getUpdatedAt())
                .build();
    }
}
