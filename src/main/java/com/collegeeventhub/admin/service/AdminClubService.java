package com.collegeeventhub.admin.service;

import com.collegeeventhub.club.dto.ClubResponse;
import com.collegeeventhub.common.response.PageResponse;

public interface AdminClubService {

    PageResponse<ClubResponse> getAllClubs(
            int page,
            int size,
            Boolean active
    );

    void activateClub(Long clubId, Long adminUserId);

    void deactivateClub(Long clubId, Long adminUserId);
}
