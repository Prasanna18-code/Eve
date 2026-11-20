package com.collegeeventhub.admin.controller;

import com.collegeeventhub.admin.service.AdminClubService;
import com.collegeeventhub.admin.security.AdminActor;
import com.collegeeventhub.club.dto.ClubResponse;
import com.collegeeventhub.common.response.ApiResponse;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/clubs")
@RequiredArgsConstructor
public class AdminClubController {

    private final AdminClubService adminClubService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ClubResponse>>> getAllClubs(
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size,
            @RequestParam(required = false) Boolean active) {
        Pagination.validate(page, size);
        PageResponse<ClubResponse> clubs =
                adminClubService.getAllClubs(page, size, active);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Clubs fetched successfully", clubs)
        );
    }

    @PutMapping("/{clubId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateClub(
            @PathVariable Long clubId,
            Authentication authentication) {
        adminClubService.activateClub(
                clubId,
                AdminActor.getUserId(authentication)
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Club activated successfully", null)
        );
    }

    @PutMapping("/{clubId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateClub(
            @PathVariable Long clubId,
            Authentication authentication) {
        adminClubService.deactivateClub(
                clubId,
                AdminActor.getUserId(authentication)
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Club deactivated successfully", null)
        );
    }
}
