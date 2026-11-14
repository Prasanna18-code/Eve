package com.collegeeventhub.club.service;

import com.collegeeventhub.club.dto.ClubRequest;
import com.collegeeventhub.club.dto.ClubResponse;

import java.util.List;

public interface ClubService {

    ClubResponse createClub(
            Long organizationId,
            String username,
            ClubRequest request
    );

    List<ClubResponse> getAllClubs();

    ClubResponse getClubById(Long clubId);

    List<ClubResponse> getClubsByOrganization(Long organizationId);

    ClubResponse updateClub(
            Long clubId,
            String username,
            ClubRequest request
    );

    void deleteClub(
            Long clubId,
            String username
    );
}
