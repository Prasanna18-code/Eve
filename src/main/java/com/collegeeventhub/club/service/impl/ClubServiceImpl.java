package com.collegeeventhub.club.service.impl;

import com.collegeeventhub.club.dto.ClubRequest;
import com.collegeeventhub.club.dto.ClubResponse;
import com.collegeeventhub.club.entity.ClubEntity;
import com.collegeeventhub.club.mapper.ClubMapper;
import com.collegeeventhub.club.repository.ClubRepository;
import com.collegeeventhub.club.service.ClubService;
import com.collegeeventhub.common.exception.DuplicateResourceException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.organization.entity.OrganizationEntity;
import com.collegeeventhub.organization.repository.OrganizationRepository;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ClubMapper clubMapper;

    @Override
    @Transactional
    public ClubResponse createClub(
            Long organizationId,
            String username,
            ClubRequest request) {

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Organization not found"));

        if (!organization.getLeader().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "Only the organization leader can create a club");
        }

        if (clubRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Club with this name already exists");
        }

        UserEntity leader = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        ClubEntity club = clubMapper.toEntity(request);
        club.setOrganization(organization);
        club.setLeader(leader);

        return clubMapper.toResponse(clubRepository.save(club));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubResponse> getAllClubs() {
        return clubRepository.findAll()
                .stream()
                .map(clubMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClubResponse getClubById(Long clubId) {
        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Club not found"));

        return clubMapper.toResponse(club);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubResponse> getClubsByOrganization(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found");
        }

        return clubRepository.findByOrganizationId(organizationId)
                .stream()
                .map(clubMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClubResponse updateClub(
            Long clubId,
            String username,
            ClubRequest request) {

        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Club not found"));

        verifyClubLeader(club, username);

        if (!club.getName().equals(request.getName())
                && clubRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Club with this name already exists");
        }

        club.setName(request.getName());
        club.setDescription(request.getDescription());
        club.setEmail(request.getEmail());
        club.setPhoneNumber(request.getPhoneNumber());
        club.setLogo(request.getLogo());

        return clubMapper.toResponse(clubRepository.save(club));
    }

    @Override
    @Transactional
    public void deleteClub(Long clubId, String username) {
        ClubEntity club = clubRepository.findById(clubId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Club not found"));

        verifyClubLeader(club, username);
        club.setActive(false);
        clubRepository.save(club);
    }

    private void verifyClubLeader(ClubEntity club, String username) {
        if (!club.getLeader().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "You are not the leader of this club");
        }
    }
}