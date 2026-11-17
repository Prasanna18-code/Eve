package com.collegeeventhub.security;

import java.io.IOException;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            JwtService.JwtIdentity tokenIdentity =
                    jwtService.parseAndValidateToken(jwt);

            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                AuthenticatedUserPrincipal userDetails =
                        userDetailsService.loadUserById(tokenIdentity.userId());

                if (!tokenIdentity.username().equals(userDetails.getUsername())
                        || !userDetails.isEnabled()) {
                    writeAuthenticationFailure(response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException
                 | UsernameNotFoundException exception) {
            writeAuthenticationFailure(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeAuthenticationFailure(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                """
                {
                    "success": false,
                    "message": "Invalid or expired token"
                }
                """
        );
    }
}
