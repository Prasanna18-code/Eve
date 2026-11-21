package com.collegeeventhub.common.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationRateLimitFilterTest {
    private AuthenticationRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.getLogin().setCapacity(1);
        properties.getLogin().setWindowSeconds(600);
        properties.getRegistration().setCapacity(1);
        properties.getRegistration().setWindowSeconds(3600);
        filter = new AuthenticationRateLimitFilter(
                new RateLimitService(properties),
                properties
        );
    }

    @Test
    void returns429WithRetryAfterForRepeatedLogin() throws Exception {
        FilterChain chain = (request, response) -> {
        };

        MockHttpServletResponse first = serve("/api/v1/auth/login", chain);
        MockHttpServletResponse second = serve("/api/v1/auth/login", chain);

        assertEquals(200, first.getStatus());
        assertEquals(429, second.getStatus());
        assertTrue(second.getHeader("Retry-After") != null
                && Integer.parseInt(second.getHeader("Retry-After")) >= 1);
    }

    @Test
    void registrationHasAnIndependentBucket() throws Exception {
        FilterChain chain = (request, response) -> {
        };

        MockHttpServletResponse first =
                serve("/api/v1/auth/register", chain);
        MockHttpServletResponse second =
                serve("/api/v1/auth/register", chain);

        assertEquals(200, first.getStatus());
        assertEquals(429, second.getStatus());
    }

    private MockHttpServletResponse serve(
            String path,
            FilterChain chain)
            throws IOException, jakarta.servlet.ServletException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("127.0.0.1");
        request.setContentType("application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
