package com.collegeeventhub.club.repository;

import com.collegeeventhub.club.entity.ClubMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMemberRepository
        extends JpaRepository<ClubMemberEntity, Long> {

    Optional<ClubMemberEntity> findByClubIdAndUserId(
            Long clubId,
            Long userId
    );

    boolean existsByClubIdAndUserId(
            Long clubId,
            Long userId
    );

    List<ClubMemberEntity> findByClubIdAndActiveTrue(
            Long clubId
    );

    List<ClubMemberEntity> findByUserIdAndActiveTrue(
            Long userId
    );
}
