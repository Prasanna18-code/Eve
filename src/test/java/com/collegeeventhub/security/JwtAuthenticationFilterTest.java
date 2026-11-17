package com.collegeeventhub.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final CustomUserDetailsService userDetailsService =
            mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(
                    new JwtService(SECRET, 60_000),
                    userDetailsService
            );
    private final FilterChain filterChain = mock(FilterChain.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenForDeletedAccountReturnsUnauthorized() throws Exception {
        String token = new JwtService(SECRET, 60_000)
                .generateToken(404L, "deletedUser");
        when(userDetailsService.loadUserById(404L))
                .thenThrow(new UsernameNotFoundException("User not found"));

        MockHttpServletResponse response = filter(token);

        assertUnauthorized(response);
        verify(userDetailsService).loadUserById(404L);
        verify(filterChain, never()).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void malformedTokenReturnsUnauthorized() throws Exception {
        MockHttpServletResponse response = filter("not-a-jwt");

        assertUnauthorized(response);
        verify(filterChain, never()).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        String token = new JwtService(SECRET, -1)
                .generateToken(42L, "expiredUser");

        MockHttpServletResponse response = filter(token);

        assertUnauthorized(response);
        verify(filterChain, never()).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private MockHttpServletResponse filter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);
        return response;
    }

    private void assertUnauthorized(MockHttpServletResponse response) {
        assertEquals(401, response.getStatus());
        assertTrue(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)
                .contains("\"message\": \"Invalid or expired token\""));
    }
}
