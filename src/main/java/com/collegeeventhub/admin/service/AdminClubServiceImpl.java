package com.collegeeventhub.admin.service;

import com.collegeeventhub.club.dto.ClubResponse;
import com.collegeeventhub.club.entity.ClubEntity;
import com.collegeeventhub.club.mapper.ClubMapper;
import com.collegeeventhub.club.repository.ClubRepository;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.common.response.PageResponse;
import com.collegeeventhub.common.util.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminClubServiceImpl implements AdminClubService {

    private final ClubRepository clubRepository;
    private final ClubMapper clubMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClubResponse> getAllClubs(
            int page,
            int size,
            Boolean active) {
        Page<ClubEntity> clubs = clubRepository.findAllByFilters(
                active != null,
                Boolean.TRUE.equals(active),
                Pagination.pageRequest(
                        page,
                        size,
                        Sort.by(Sort.Order.asc("id"))
                )
        );
        return PageResponse.from(clubs, clubMapper::toResponse);
    }

    @Override
    @Transactional
    public void activateClub(Long clubId) {
        ClubEntity club = findClub(clubId);
        club.setActive(true);
        clubRepository.save(club);
    }

    @Override
    @Transactional
    public void deactivateClub(Long clubId) {
        ClubEntity club = findClub(clubId);
        club.setActive(false);
        clubRepository.save(club);
    }

    private ClubEntity findClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Club not found"));
    }
}
