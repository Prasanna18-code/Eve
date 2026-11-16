package com.collegeeventhub.event.service;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.event.entity.EventEntity;
import com.collegeeventhub.user.entity.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class EventEligibilityService {

    public boolean isEligible(UserEntity user, EventEntity event) {
        if (user.getRole() != Role.STUDENT) {
            return true;
        }

        return matches(event.getEligibleBranches(), user.getBranch())
                && matches(event.getEligibleAcademicYears(), user.getAcademicYear())
                && matches(event.getEligibleSections(), user.getSection());
    }

    public void checkEligible(UserEntity user, EventEntity event) {
        if (!isEligible(user, event)) {
            throw new AccessDeniedException(
                    "You are not eligible for this event"
            );
        }
    }

    private <T> boolean matches(Iterable<T> targets, T profileValue) {
        if (targets == null) {
            return true;
        }

        var iterator = targets.iterator();
        if (!iterator.hasNext()) {
            return true;
        }
        if (profileValue == null) {
            return false;
        }

        do {
            if (profileValue.equals(iterator.next())) {
                return true;
            }
        } while (iterator.hasNext());

        return false;
    }
}
