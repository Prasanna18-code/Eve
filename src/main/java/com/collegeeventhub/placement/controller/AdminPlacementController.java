package com.collegeeventhub.placement.controller;

import com.collegeeventhub.admin.security.AdminActor;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import com.collegeeventhub.placement.dto.PlacementApplicantResponse;
import com.collegeeventhub.placement.dto.PlacementRequest;
import com.collegeeventhub.placement.dto.PlacementResponse;
import com.collegeeventhub.placement.entity.PlacementStatus;
import com.collegeeventhub.placement.service.PlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/placements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlacementController {

    private final PlacementService placementService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlacementResponse>> createPlacement(
            @Valid @RequestBody PlacementRequest request,
            Authentication authentication) {
        PlacementResponse placement = placementService.createPlacement(
                request,
                AdminActor.getUserId(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Placement created successfully",
                placement
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlacementResponse>>>
    getPlacements(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) PlacementStatus status) {
        Pagination.validate(page, size);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placements fetched successfully",
                placementService.getAdminPlacements(page, size, status)
        ));
    }

    @GetMapping("/{placementId}")
    public ResponseEntity<ApiResponse<PlacementResponse>> getPlacement(
            @PathVariable Long placementId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placement fetched successfully",
                placementService.getAdminPlacement(placementId)
        ));
    }

    @PutMapping("/{placementId}")
    public ResponseEntity<ApiResponse<PlacementResponse>> updatePlacement(
            @PathVariable Long placementId,
            @Valid @RequestBody PlacementRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placement updated successfully",
                placementService.updatePlacement(
                        placementId,
                        request,
                        AdminActor.getUserId(authentication)
                )
        ));
    }

    @PutMapping("/{placementId}/publish")
    public ResponseEntity<ApiResponse<PlacementResponse>> publishPlacement(
            @PathVariable Long placementId,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placement published successfully",
                placementService.publishPlacement(
                        placementId,
                        AdminActor.getUserId(authentication)
                )
        ));
    }

    @PutMapping("/{placementId}/close")
    public ResponseEntity<ApiResponse<PlacementResponse>> closePlacement(
            @PathVariable Long placementId,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placement closed successfully",
                placementService.closePlacement(
                        placementId,
                        AdminActor.getUserId(authentication)
                )
        ));
    }

    @GetMapping("/{placementId}/applications")
    public ResponseEntity<ApiResponse<PageResponse<PlacementApplicantResponse>>>
    getApplicants(
            @PathVariable Long placementId,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size) {
        Pagination.validate(page, size);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Placement applicants fetched successfully",
                placementService.getApplicants(placementId, page, size)
        ));
    }
}
