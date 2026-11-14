package com.collegeeventhub.common.security;

import com.collegeeventhub.common.enums.Role;
import com.collegeeventhub.common.exception.ResourceNotFoundException;
import com.collegeeventhub.user.entity.UserEntity;
import com.collegeeventhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentProfileVerificationGuard {

    public static final String VERIFICATION_REQUIRED_MESSAGE =
            "Please verify your student profile before accessing student events.";

    private final UserRepository userRepository;

    public void checkAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        UserEntity user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        checkUser(user);
    }

    public void checkUser(UserEntity user) {
        if (user.getRole() == Role.STUDENT && !user.isProfileVerified()) {
            throw new AccessDeniedException(VERIFICATION_REQUIRED_MESSAGE);
        }
    }
}
