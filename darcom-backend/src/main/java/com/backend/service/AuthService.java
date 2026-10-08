package com.backend.service;

import com.backend.domain.RefreshToken;
import com.backend.domain.User;
import com.backend.domain.enums.UserRole;
import com.backend.exception.ConflictException;
import com.backend.exception.ForbiddenException;
import com.backend.exception.UnauthorizedException;
import com.backend.repository.RefreshTokenRepository;
import com.backend.repository.UserRepository;
import com.backend.security.JwtService;
import com.backend.security.PasswordHasher;
import com.backend.security.RefreshTokenHasher;
import jakarta.persistence.EntityManager;

import java.time.Duration;
import java.time.Instant;

public class AuthService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    /** POST /auth/register */
    public User register(User newUser, String rawPassword) {
        if (newUser.getRole() != UserRole.HOST && newUser.getRole() != UserRole.VISITOR) {
            throw new ForbiddenException("INVALID_ROLE", "Only HOST or VISITOR may self-register");
        }

        return TransactionRunner.call(em -> {
            UserRepository userRepository = new UserRepository(em);
            if (userRepository.existsByEmail(newUser.getEmail())) {
                throw new ConflictException("EMAIL_ALREADY_EXISTS", "An account with this email already exists");
            }

            newUser.setPasswordHash(PasswordHasher.hash(rawPassword));
            newUser.setVerified(false);
            newUser.setBlocked(false);

            return userRepository.create(newUser);
        });
    }

    /** POST /auth/login */
    public AuthResult login(String email, String rawPassword) {
        return TransactionRunner.call(em -> {
            User user = new UserRepository(em).findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password"));

            // Same error whether the email doesn't exist or the password is wrong (§3.4) —
            // checked in this order so a wrong password never reveals account state either.
            if (!PasswordHasher.matches(rawPassword, user.getPasswordHash())) {
                throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
            }
            if (user.isBlocked()) {
                throw new ForbiddenException("ACCOUNT_BLOCKED", "This account has been blocked");
            }

            String accessToken = JwtService.issueAccessToken(user);
            String refreshToken = issueRefreshToken(em, user);

            return new AuthResult(accessToken, refreshToken, user);
        });
    }

    /** POST /auth/refresh — spec-literal, NO rotation: the presented token stays valid, only a fresh access token is returned. */
    public RefreshResult refresh(String rawRefreshToken) {
        return TransactionRunner.call(em -> {
            RefreshTokenRepository refreshTokenRepository = new RefreshTokenRepository(em);
            String hash = RefreshTokenHasher.hash(rawRefreshToken);

            RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                    .orElseThrow(() -> new UnauthorizedException("INVALID_OR_EXPIRED_REFRESH_TOKEN", "Refresh token not recognized"));

            if (existing.isExpired()) {
                refreshTokenRepository.delete(existing);
                throw new UnauthorizedException("INVALID_OR_EXPIRED_REFRESH_TOKEN", "Refresh token has expired");
            }

            User user = existing.getUser();
            String accessToken = JwtService.issueAccessToken(user);

            return new RefreshResult(accessToken, JwtService.ACCESS_TOKEN_TTL.getSeconds());
        });
    }

    /** POST /auth/logout — idempotent: an already-invalid token is not an error */
    public void logout(String rawRefreshToken) {
        TransactionRunner.execute(em -> {
            String hash = RefreshTokenHasher.hash(rawRefreshToken);
            new RefreshTokenRepository(em).deleteByTokenHash(hash);
        });
    }

    private String issueRefreshToken(EntityManager em, User user) {
        String raw = RefreshTokenHasher.generateRaw();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(RefreshTokenHasher.hash(raw));
        refreshToken.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_TTL));

        new RefreshTokenRepository(em).create(refreshToken);
        return raw;
    }
}
