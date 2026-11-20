package com.collegeeventhub.placement.controller;

import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.placement.dto.PlacementApplicationResponse;
import com.collegeeventhub.placement.dto.PlacementResponse;
import com.collegeeventhub.placement.entity.PlacementType;
import com.collegeeventhub.placement.service.PlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/placements")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlacementResponse>>>
    getPublishedPlacements(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) PlacementType type) {
        Pagination.validate(page, size);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placements fetched successfully",
                placementService.getPublishedPlacements(page, size, type)
        ));
    }

    @GetMapping("/{placementId}")
    public ResponseEntity<ApiResponse<PlacementResponse>> getPlacement(
            @PathVariable Long placementId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placement fetched successfully",
                placementService.getPublishedPlacement(placementId)
        ));
    }

    @PostMapping("/{placementId}/applications")
    public ResponseEntity<ApiResponse<PlacementApplicationResponse>> apply(
            @PathVariable Long placementId,
            Authentication authentication) {
        PlacementApplicationResponse application = placementService.apply(
                placementId,
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Placement application submitted successfully",
                application
        ));
    }

    @GetMapping("/applications/me")
    public ResponseEntity<ApiResponse<PageResponse<PlacementApplicationResponse>>>
    getMyApplications(
            Authentication authentication,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size) {
        Pagination.validate(page, size);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placement applications fetched successfully",
                placementService.getMyApplications(
                        page,
                        size,
                        authentication.getName()
                )
        ));
    }
}
