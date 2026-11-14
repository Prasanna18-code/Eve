package com.collegeeventhub.club.mapper;

import com.collegeeventhub.club.dto.ClubMemberResponse;
import com.collegeeventhub.club.entity.ClubMemberEntity;
import org.springframework.stereotype.Component;

@Component
public class ClubMemberMapper {

    public ClubMemberResponse toResponse(ClubMemberEntity member) {
        return ClubMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .firstName(member.getUser().getFirstName())
                .lastName(member.getUser().getLastName())
                .email(member.getUser().getEmail())
                .active(member.getActive())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
