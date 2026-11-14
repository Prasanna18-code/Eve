package com.collegeeventhub.club.service;

import com.collegeeventhub.club.dto.ClubMemberRequest;
import com.collegeeventhub.club.dto.ClubMemberResponse;
import com.collegeeventhub.club.entity.ClubEntity;
import com.collegeeventhub.club.entity.ClubMemberEntity;
import com.collegeeventhub.club.mapper.ClubMemberMapper;
import com.collegeeventhub.club.repository.ClubMemberRepository;
import com.collegeeventhub.club.repository.ClubRepository;
import com.collegeeventhub.club.service.impl.ClubMemberServiceImpl;
import com.collegeeventhub.common.exception.ConflictException;
import com.collegeeventhub.common.exception.DuplicateResourceException;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceImplTest {

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubMemberMapper clubMemberMapper;

    @InjectMocks
    private ClubMemberServiceImpl clubMemberService;

    private UserEntity leader;
    private UserEntity inactiveUser;
    private ClubEntity activeClub;

    @BeforeEach
    void setUp() {
        leader = UserEntity.builder()
                .id(1L)
                .username("clubleader")
                .active(true)
                .build();

        inactiveUser = UserEntity.builder()
                .id(2L)
                .username("inactiveuser")
                .active(false)
                .build();

        activeClub = ClubEntity.builder()
                .id(10L)
                .active(true)
                .leader(leader)
                .build();
    }

    @Test
    void addMemberRejectsInactiveUserWithoutSavingMembership() {
        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(inactiveUser));

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> clubMemberService.addMember(10L, "clubleader", request)
        );

        assertEquals(
                "Cannot add an inactive user to a club",
                exception.getMessage()
        );
        verify(clubMemberRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
        verifyNoInteractions(clubMemberRepository, clubMemberMapper);
    }

    @Test
    void addMemberRejectsInactiveClubBeforeLoadingUsers() {
        activeClub.setActive(false);
        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> clubMemberService.addMember(10L, "clubleader", request)
        );

        assertEquals(
                "Cannot add members to an inactive club",
                exception.getMessage()
        );
        verifyNoInteractions(
                userRepository,
                clubMemberRepository,
                clubMemberMapper
        );
    }

    @Test
    void addMemberRejectsAlreadyActiveMember() {
        UserEntity activeUser = UserEntity.builder()
                .id(2L)
                .username("activeuser")
                .active(true)
                .build();
        ClubMemberEntity existingMembership = ClubMemberEntity.builder()
                .id(20L)
                .club(activeClub)
                .user(activeUser)
                .active(true)
                .build();

        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(activeUser));
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(existingMembership));

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> clubMemberService.addMember(10L, "clubleader", request)
        );

        assertEquals(
                "User is already a member of this club",
                exception.getMessage()
        );
        verify(clubMemberRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
        verifyNoInteractions(clubMemberMapper);
    }

    @Test
    void addMemberReactivatesExistingInactiveMembership() {
        UserEntity activeUser = UserEntity.builder()
                .id(2L)
                .username("activeuser")
                .active(true)
                .build();
        ClubMemberEntity existingMembership = ClubMemberEntity.builder()
                .id(20L)
                .club(activeClub)
                .user(activeUser)
                .active(false)
                .build();

        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(activeUser));
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(existingMembership));
        when(clubMemberRepository.save(existingMembership))
                .thenReturn(existingMembership);

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        clubMemberService.addMember(10L, "clubleader", request);

        assertTrue(existingMembership.getActive());
        verify(clubMemberRepository).save(same(existingMembership));
        verify(clubMemberMapper).toResponse(existingMembership);
    }

    @Test
    void addMemberRejectsNonLeaderBeforeLoadingTargetUser() {
        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> clubMemberService.addMember(10L, "anotheruser", request)
        );

        assertEquals(
                "You are not the leader of this club",
                exception.getMessage()
        );
        verifyNoInteractions(
                userRepository,
                clubMemberRepository,
                clubMemberMapper
        );
    }

    @Test
    void removeMemberByLeaderSoftDeletesMembership() {
        UserEntity memberUser = UserEntity.builder()
                .id(2L)
                .username("member")
                .active(true)
                .build();
        ClubMemberEntity existingMembership = ClubMemberEntity.builder()
                .id(20L)
                .club(activeClub)
                .user(memberUser)
                .active(true)
                .build();

        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(existingMembership));

        clubMemberService.removeMember(10L, 2L, "clubleader");

        assertFalse(existingMembership.getActive());
        verify(clubMemberRepository).save(same(existingMembership));
        verify(clubMemberRepository, never()).delete(existingMembership);
    }

    @Test
    void removeMemberRejectsNonLeaderWithoutChangingMembership() {
        UserEntity memberUser = UserEntity.builder()
                .id(2L)
                .username("member")
                .active(true)
                .build();
        ClubMemberEntity existingMembership = ClubMemberEntity.builder()
                .id(20L)
                .club(activeClub)
                .user(memberUser)
                .active(true)
                .build();

        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));

        assertThrows(
                AccessDeniedException.class,
                () -> clubMemberService.removeMember(10L, 2L, "anotheruser")
        );

        assertTrue(existingMembership.getActive());
        verifyNoInteractions(clubMemberRepository);
    }

    @Test
    void addMemberSuccessfullyCreatesMembership() {
        UserEntity activeUser = UserEntity.builder()
                .id(2L)
                .username("activeuser")
                .active(true)
                .build();

        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(activeUser));
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L))
                .thenReturn(Optional.empty());
        when(clubMemberRepository.save(any(ClubMemberEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        clubMemberService.addMember(10L, "clubleader", request);

        ArgumentCaptor<ClubMemberEntity> captor =
                ArgumentCaptor.forClass(ClubMemberEntity.class);
        verify(clubMemberRepository).save(captor.capture());

        ClubMemberEntity savedMembership = captor.getValue();
        assertTrue(savedMembership.getActive());
        assertEquals(activeClub, savedMembership.getClub());
        assertEquals(activeUser, savedMembership.getUser());
        verify(clubMemberMapper).toResponse(same(savedMembership));
    }

    @Test
    void addMemberThrowsWhenClubDoesNotExist() {
        when(clubRepository.findById(10L))
                .thenReturn(Optional.empty());

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clubMemberService.addMember(10L, "clubleader", request)
        );

        assertEquals("Club not found", exception.getMessage());
        verifyNoInteractions(
                userRepository,
                clubMemberRepository,
                clubMemberMapper
        );
    }

    @Test
    void addMemberThrowsWhenUserDoesNotExist() {
        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        ClubMemberRequest request = ClubMemberRequest.builder()
                .userId(2L)
                .build();

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clubMemberService.addMember(10L, "clubleader", request)
        );

        assertEquals("User not found", exception.getMessage());
        verifyNoInteractions(clubMemberRepository, clubMemberMapper);
    }

    @Test
    void getClubMembersReturnsActiveMemberships() {
        UserEntity memberUser = UserEntity.builder()
                .id(2L)
                .username("member")
                .active(true)
                .build();
        ClubMemberEntity membership = ClubMemberEntity.builder()
                .id(20L)
                .club(activeClub)
                .user(memberUser)
                .active(true)
                .build();

        when(clubRepository.existsById(10L))
                .thenReturn(true);
        when(clubMemberRepository.findByClubIdAndActiveTrue(10L))
                .thenReturn(List.of(membership));

        List<ClubMemberResponse> result =
                clubMemberService.getClubMembers(10L);

        assertEquals(1, result.size());
        verify(clubMemberMapper).toResponse(same(membership));
    }

    @Test
    void getClubMembersThrowsWhenClubDoesNotExist() {
        when(clubRepository.existsById(10L))
                .thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clubMemberService.getClubMembers(10L)
        );

        assertEquals("Club not found", exception.getMessage());
        verifyNoInteractions(clubMemberRepository, clubMemberMapper);
    }

    @Test
    void removeMemberThrowsWhenClubDoesNotExist() {
        when(clubRepository.findById(10L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clubMemberService.removeMember(10L, 2L, "clubleader")
        );

        assertEquals("Club not found", exception.getMessage());
        verifyNoInteractions(clubMemberRepository);
    }

    @Test
    void removeMemberThrowsWhenMembershipDoesNotExist() {
        when(clubRepository.findById(10L))
                .thenReturn(Optional.of(activeClub));
        when(clubMemberRepository.findByClubIdAndUserId(10L, 2L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clubMemberService.removeMember(10L, 2L, "clubleader")
        );

        assertEquals("Club member not found", exception.getMessage());
        verify(clubMemberRepository, never()).save(any());
    }
}
