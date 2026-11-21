package com.collegeeventhub.config;

import com.collegeeventhub.common.ratelimit.AuthenticationRateLimitFilter;
import com.collegeeventhub.common.ratelimit.RateLimitProperties;
import com.collegeeventhub.common.ratelimit.RateLimitService;
import com.collegeeventhub.security.JwtAuthenticationFilter;
import com.collegeeventhub.common.security.CustomAccessDeniedHandler;
import com.collegeeventhub.common.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    AuthenticationRateLimitFilter authenticationRateLimitFilter(Environment environment) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(
                environment.getProperty("rate-limit.enabled", Boolean.class, true)
        );
        properties.getLogin().setCapacity(
                environment.getProperty("rate-limit.login.capacity", Integer.class, 10)
        );
        properties.getLogin().setWindowSeconds(
                environment.getProperty("rate-limit.login.window-seconds", Long.class, 600L)
        );
        properties.getRegistration().setCapacity(
                environment.getProperty("rate-limit.registration.capacity", Integer.class, 5)
        );
        properties.getRegistration().setWindowSeconds(
                environment.getProperty("rate-limit.registration.window-seconds", Long.class, 3600L)
        );

        return new AuthenticationRateLimitFilter(
                new RateLimitService(properties),
                properties
        );
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationRateLimitFilter authenticationRateLimitFilter) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            );

        http.addFilterBefore(
                authenticationRateLimitFilter,
                UsernamePasswordAuthenticationFilter.class
        );
        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
