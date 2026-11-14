package com.collegeeventhub.club.service;

import com.collegeeventhub.club.dto.ClubMemberRequest;
import com.collegeeventhub.club.dto.ClubMemberResponse;

import java.util.List;

public interface ClubMemberService {

    ClubMemberResponse addMember(
            Long clubId,
            String username,
            ClubMemberRequest request
    );

    List<ClubMemberResponse> getClubMembers(
            Long clubId
    );

    void removeMember(
            Long clubId,
            Long userId,
            String username
    );
}
