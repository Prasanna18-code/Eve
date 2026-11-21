package com.collegeeventhub.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;

    public AuthenticationRateLimitFilter(
            RateLimitService rateLimitService,
            RateLimitProperties properties) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!properties.isEnabled()
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        RateLimitService.Decision decision;

        if ("/api/v1/auth/login".equals(path)) {
            decision = rateLimitService.tryConsumeLogin(
                    request.getRemoteAddr()
            );
        } else if ("/api/v1/auth/register".equals(path)) {
            decision = rateLimitService.tryConsumeRegistration(
                    request.getRemoteAddr()
            );
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                """
                {"success":false,"message":"Too many requests","data":null}
                """
        );
    }

}
