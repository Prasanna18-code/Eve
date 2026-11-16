package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventEligibilityServiceTest {

    private final EventEligibilityService eligibilityService =
            new EventEligibilityService();

    @Test
    void studentMatchingEveryTargetCriterionIsEligible() {
        assertTrue(eligibilityService.isEligible(
                student("CSE", 3, "A"),
                event(Set.of("CSE", "ECE"), Set.of(2, 3), Set.of("A", "B"))
        ));
    }

    @Test
    void branchMismatchIsIneligible() {
        assertFalse(eligibilityService.isEligible(
                student("ME", 3, "A"),
                event(Set.of("CSE"), Set.of(), Set.of())
        ));
    }

    @Test
    void academicYearMismatchIsIneligible() {
        assertFalse(eligibilityService.isEligible(
                student("CSE", 4, "A"),
                event(Set.of(), Set.of(2, 3), Set.of())
        ));
    }

    @Test
    void sectionMismatchIsIneligible() {
        assertFalse(eligibilityService.isEligible(
                student("CSE", 3, "B"),
                event(Set.of(), Set.of(), Set.of("A"))
        ));
    }

    @Test
    void emptyTargetCollectionsAllowAllVerifiedStudents() {
        assertTrue(eligibilityService.isEligible(
                student("ME", 1, "Z"),
                event(Set.of(), Set.of(), Set.of())
        ));
    }

    @Test
    void everyPopulatedCriterionMustMatch() {
        EventEntity event = event(Set.of("CSE"), Set.of(3), Set.of("A"));

        assertTrue(eligibilityService.isEligible(student("CSE", 3, "A"), event));
        assertFalse(eligibilityService.isEligible(student("ECE", 3, "A"), event));
        assertFalse(eligibilityService.isEligible(student("CSE", 2, "A"), event));
        assertFalse(eligibilityService.isEligible(student("CSE", 3, "B"), event));
    }

    @Test
    void missingProfileValueDoesNotMatchPopulatedCriterion() {
        assertFalse(eligibilityService.isEligible(
                student(null, 3, "A"),
                event(Set.of("CSE"), Set.of(), Set.of())
        ));
    }

    @Test
    void nonStudentRolesRetainUnfilteredAccess() {
        UserEntity clubAdmin = UserEntity.builder()
                .role(Role.CLUB_ADMIN)
                .branch("ME")
                .academicYear(1)
                .section("Z")
                .build();
        UserEntity admin = UserEntity.builder()
                .role(Role.ADMIN)
                .build();
        EventEntity targetedEvent = event(
                Set.of("CSE"),
                Set.of(3),
                Set.of("A")
        );

        assertTrue(eligibilityService.isEligible(clubAdmin, targetedEvent));
        assertTrue(eligibilityService.isEligible(admin, targetedEvent));
    }

    @Test
    void checkEligibleRejectsIneligibleStudent() {
        assertThrows(
                AccessDeniedException.class,
                () -> eligibilityService.checkEligible(
                        student("ME", 3, "A"),
                        event(Set.of("CSE"), Set.of(), Set.of())
                )
        );
    }

    private UserEntity student(String branch, Integer year, String section) {
        return UserEntity.builder()
                .role(Role.STUDENT)
                .profileVerified(true)
                .branch(branch)
                .academicYear(year)
                .section(section)
                .build();
    }

    private EventEntity event(
            Set<String> branches,
            Set<Integer> years,
            Set<String> sections) {
        return EventEntity.builder()
                .eligibleBranches(branches)
                .eligibleAcademicYears(years)
                .eligibleSections(sections)
                .build();
    }
}
