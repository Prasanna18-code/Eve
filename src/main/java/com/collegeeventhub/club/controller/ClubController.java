package com.collegeeventhub.club.controller;

import com.collegeeventhub.club.dto.ClubRequest;
import com.collegeeventhub.club.dto.ClubResponse;
import com.collegeeventhub.club.service.ClubService;
import com.collegeeventhub.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping("/organizations/{organizationId}/clubs")
    public ResponseEntity<ApiResponse<ClubResponse>> createClub(
            @PathVariable Long organizationId,
            @Valid @RequestBody ClubRequest request,
            Authentication authentication) {

        ClubResponse response = clubService.createClub(
                organizationId,
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Club created successfully", response)
        );
    }

    @GetMapping("/clubs")
    public ResponseEntity<ApiResponse<List<ClubResponse>>> getAllClubs() {

        List<ClubResponse> clubs = clubService.getAllClubs();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Clubs fetched successfully", clubs)
        );
    }

    @GetMapping("/clubs/{clubId}")
    public ResponseEntity<ApiResponse<ClubResponse>> getClubById(
            @PathVariable Long clubId) {

        ClubResponse response = clubService.getClubById(clubId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Club fetched successfully", response)
        );
    }

    @GetMapping("/organizations/{organizationId}/clubs")
    public ResponseEntity<ApiResponse<List<ClubResponse>>> getClubsByOrganization(
            @PathVariable Long organizationId) {

        List<ClubResponse> clubs =
                clubService.getClubsByOrganization(organizationId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Organization clubs fetched successfully",
                        clubs
                )
        );
    }

    @PutMapping("/clubs/{clubId}")
    public ResponseEntity<ApiResponse<ClubResponse>> updateClub(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubRequest request,
            Authentication authentication) {

        ClubResponse response = clubService.updateClub(
                clubId,
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Club updated successfully", response)
        );
    }

    @DeleteMapping("/clubs/{clubId}")
    public ResponseEntity<ApiResponse<Void>> deleteClub(
            @PathVariable Long clubId,
            Authentication authentication) {

        clubService.deleteClub(
                clubId,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Club deleted successfully", null)
        );
    }
}
