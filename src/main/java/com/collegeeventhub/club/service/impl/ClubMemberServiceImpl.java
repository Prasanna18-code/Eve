package com.collegeeventhub.club.service.impl;

import com.collegeeventhub.club.dto.ClubMemberRequest;
import com.collegeeventhub.club.dto.ClubMemberResponse;
import com.collegeeventhub.club.entity.ClubEntity;
import com.collegeeventhub.club.entity.ClubMemberEntity;
import com.collegeeventhub.club.mapper.ClubMemberMapper;
import com.collegeeventhub.club.repository.ClubMemberRepository;
import com.collegeeventhub.club.repository.ClubRepository;
import com.collegeeventhub.club.service.ClubMemberService;
import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.common.exception.DuplicateResourceException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubMemberServiceImpl implements ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubMemberMapper clubMemberMapper;

    @Override
    @Transactional
    public ClubMemberResponse addMember(
            Long clubId,
            String username,
            ClubMemberRequest request) {

        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Club not found"));

        if (!Boolean.TRUE.equals(club.getActive())) {
            throw new ConflictException(
                    "Cannot add members to an inactive club");
        }

        verifyClubLeader(club, username);

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ConflictException(
                    "Cannot add an inactive user to a club");
        }

        return clubMemberRepository.findByClubIdAndUserId(clubId, user.getId())
                .map(existing -> {
                    if (existing.getActive()) {
                        throw new DuplicateResourceException(
                                "User is already a member of this club");
                    }

                    existing.setActive(true);
                    return clubMemberMapper.toResponse(
                            clubMemberRepository.save(existing));
                })
                .orElseGet(() -> {
                    ClubMemberEntity member = ClubMemberEntity.builder()
                            .club(club)
                            .user(user)
                            .active(true)
                            .build();

                    return clubMemberMapper.toResponse(
                            clubMemberRepository.save(member));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubMemberResponse> getClubMembers(Long clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ResourceNotFoundException("Club not found");
        }

        return clubMemberRepository.findByClubIdAndActiveTrue(clubId)
                .stream()
                .map(clubMemberMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeMember(
            Long clubId,
            Long userId,
            String username) {

        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Club not found"));

        verifyClubLeader(club, username);

        ClubMemberEntity member = clubMemberRepository
                .findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Club member not found"));

        member.setActive(false);
        clubMemberRepository.save(member);
    }

    private void verifyClubLeader(ClubEntity club, String username) {
        if (!club.getLeader().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "You are not the leader of this club");
        }
    }
}
