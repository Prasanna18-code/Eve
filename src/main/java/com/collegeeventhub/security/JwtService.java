package com.collegeeventhub.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public JwtIdentity parseAndValidateToken(String token) {
        io.jsonwebtoken.Claims claims = parseClaims(token);
        String username = claims.getSubject();
        String userId = claims.get("userId", String.class);
        Date tokenExpiration = claims.getExpiration();

        if (username == null || userId == null || tokenExpiration == null
                || !tokenExpiration.after(new Date())) {
            throw new io.jsonwebtoken.JwtException("Invalid JWT claims");
        }

        try {
            return new JwtIdentity(Long.parseLong(userId), username);
        } catch (NumberFormatException exception) {
            throw new io.jsonwebtoken.JwtException(
                    "Token contains an invalid user ID",
                    exception
            );
        }
    }

    private io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record JwtIdentity(Long userId, String username) {
    }
}
