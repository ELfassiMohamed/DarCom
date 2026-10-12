package com.backend.security;

import com.backend.config.AppConfig;
import com.backend.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT access/refresh tokens (HS256) using the jjwt 0.13 API.
 * Only {@code io.jsonwebtoken} API types are referenced.
 */
@Singleton
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    @Inject
    public JwtService(AppConfig config) {
        this(config.getJwtSecret(), config.getJwtIssuer(),
                config.getJwtAccessTtlSeconds(), config.getJwtRefreshTtlSeconds());
    }

    /** Direct constructor (tests and tooling). The secret must be ≥ 32 bytes. */
    public JwtService(String secret, String issuer, long accessTtlSeconds, long refreshTtlSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer).subject(user.getId().toString())
                .claim("role", user.getRole().name()).claim("typ", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key).compact();
    }

    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer).subject(user.getId().toString())
                .claim("typ", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .signWith(key).compact();
    }

    /** Verifies signature, expiry, issuer and {@code typ=access}; returns subject + role. */
    public AccessClaims parseAccess(String token) {
        Claims claims = parse(token, "access");
        String role = claims.get("role", String.class);
        if (role == null) {
            throw new InvalidTokenException("Access token has no role");
        }
        try {
            return new AccessClaims(UUID.fromString(claims.getSubject()), role);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Access token has an invalid subject", e);
        }
    }

    /** Verifies signature, expiry, issuer and {@code typ=refresh}; returns the subject UUID. */
    public UUID parseRefresh(String token) {
        Claims claims = parse(token, "refresh");
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Refresh token has an invalid subject", e);
        }
    }

    private Claims parse(String token, String expectedTyp) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).requireIssuer(issuer)
                    .build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid token", e);
        }
        if (!expectedTyp.equals(claims.get("typ", String.class))) {
            throw new InvalidTokenException("Wrong token type");
        }
        return claims;
    }

    public record AccessClaims(UUID userId, String role) {
    }
}
