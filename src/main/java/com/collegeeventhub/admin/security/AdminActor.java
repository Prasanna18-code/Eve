package com.collegeeventhub.admin.security;

import com.collegeeventhub.security.AuthenticatedUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

public final class AdminActor {

    private AdminActor() {
    }

    public static Long getUserId(Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal()
                instanceof AuthenticatedUserPrincipal principal) {
            return principal.getId();
        }
        throw new AccessDeniedException(
                "Authenticated user identity is unavailable");
    }
}
