package com.collegeeventhub.placement.service;

import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.placement.dto.PlacementApplicantResponse;
import com.collegeeventhub.placement.dto.PlacementApplicationResponse;
import com.collegeeventhub.placement.dto.PlacementRequest;
import com.collegeeventhub.placement.dto.PlacementResponse;
import com.collegeeventhub.placement.entity.PlacementStatus;
import com.collegeeventhub.placement.entity.PlacementType;

public interface PlacementService {

    PageResponse<PlacementResponse> getPublishedPlacements(
            int page,
            int size,
            PlacementType type
    );

    PlacementResponse getPublishedPlacement(Long placementId);

    PlacementApplicationResponse apply(
            Long placementId,
            String username
    );

    PageResponse<PlacementApplicationResponse> getMyApplications(
            int page,
            int size,
            String username
    );

    PageResponse<PlacementResponse> getAdminPlacements(
            int page,
            int size,
            PlacementStatus status
    );

    PlacementResponse getAdminPlacement(Long placementId);

    PlacementResponse createPlacement(
            PlacementRequest request,
            Long adminUserId
    );

    PlacementResponse updatePlacement(
            Long placementId,
            PlacementRequest request,
            Long adminUserId
    );

    PlacementResponse publishPlacement(Long placementId, Long adminUserId);

    PlacementResponse closePlacement(Long placementId, Long adminUserId);

    PageResponse<PlacementApplicantResponse> getApplicants(
            Long placementId,
            int page,
            int size
    );
}
