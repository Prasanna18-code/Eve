package com.collegeeventhub.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "0123456789abcdef0123456789abcdef",
            60_000
    );

    @Test
    void tokenIncludesSignedUserIdAndValidatesItWithUsername() {
        String token = jwtService.generateToken(42L, "student42");

        JwtService.JwtIdentity identity =
                jwtService.parseAndValidateToken(token);

        assertEquals(42L, identity.userId());
        assertEquals("student42", identity.username());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiredTokenService = new JwtService(
                "0123456789abcdef0123456789abcdef",
                -1
        );
        String token = expiredTokenService.generateToken(42L, "student42");

        assertFalse(isTokenAccepted(token));
    }

    private boolean isTokenAccepted(String token) {
        try {
            jwtService.parseAndValidateToken(token);
            return true;
        } catch (io.jsonwebtoken.JwtException exception) {
            return false;
        }
    }
}
