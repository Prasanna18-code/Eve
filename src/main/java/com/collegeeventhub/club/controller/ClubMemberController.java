package com.collegeeventhub.club.controller;

import com.collegeeventhub.club.dto.ClubMemberRequest;
import com.collegeeventhub.club.dto.ClubMemberResponse;
import com.collegeeventhub.club.service.ClubMemberService;
import com.collegeeventhub.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clubs")
@RequiredArgsConstructor
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    @PostMapping("/{clubId}/members")
    public ResponseEntity<ApiResponse<ClubMemberResponse>> addMember(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubMemberRequest request,
            Authentication authentication) {

        ClubMemberResponse response = clubMemberService.addMember(
                clubId,
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Club member added successfully",
                        response
                )
        );
    }

    @GetMapping("/{clubId}/members")
    public ResponseEntity<ApiResponse<List<ClubMemberResponse>>> getClubMembers(
            @PathVariable Long clubId) {

        List<ClubMemberResponse> members =
                clubMemberService.getClubMembers(clubId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Club members fetched successfully",
                        members
                )
        );
    }

    @DeleteMapping("/{clubId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long clubId,
            @PathVariable Long userId,
            Authentication authentication) {

        clubMemberService.removeMember(
                clubId,
                userId,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Club member removed successfully",
                        null
                )
        );
    }
}
