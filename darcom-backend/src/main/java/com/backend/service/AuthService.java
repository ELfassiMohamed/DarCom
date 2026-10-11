package com.backend.service;

import com.backend.config.AppConfig;
import com.backend.domain.RefreshToken;
import com.backend.domain.User;
import com.backend.domain.enums.UserRole;
import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.LoginResponse;
import com.backend.dto.auth.LoginUserDto;
import com.backend.dto.auth.LogoutRequest;
import com.backend.dto.auth.RefreshRequest;
import com.backend.dto.auth.RefreshResponse;
import com.backend.dto.auth.RegisterRequest;
import com.backend.dto.auth.RegisterResponse;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.persistence.Tx;
import com.backend.repository.RefreshTokenRepository;
import com.backend.repository.UserRepository;
import com.backend.security.InvalidTokenException;
import com.backend.security.JwtService;
import com.backend.security.PasswordService;
import com.backend.security.TokenHasher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.PersistenceException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/** Single place for auth side effects (a later email phase will hook post-register here). */
@Singleton
public class AuthService {

    private final AppConfig config;
    private final JwtService jwtService;
    private final PasswordService passwordService;

    @Inject
    public AuthService(AppConfig config, JwtService jwtService, PasswordService passwordService) {
        this.config = config;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        passwordService.validate(request.password());
        UserRole role = UserRole.valueOf(request.role());

        try {
            return Tx.inTransaction(em -> {
                UserRepository users = new UserRepository(em);
                if (users.existsByEmail(email)) {
                    throw new ApiException(409, ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered");
                }
                User user = new User();
                user.setEmail(email);
                user.setPasswordHash(passwordService.hash(request.password()));
                user.setFullName(request.fullName());
                user.setRole(role);
                user.setVerified(false);
                user.setBlocked(false);
                users.create(user);
                return new RegisterResponse(user.getId(), user.getEmail(), user.getFullName(),
                        user.getRole().name(), user.isVerified(), user.getCreatedAt());
            });
        } catch (PersistenceException e) {
            if (isUniqueViolation(e)) {
                throw new ApiException(409, ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered");
            }
            throw e;
        }
    }

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String rawPassword = request.password();

        return Tx.inTransaction(em -> {
            UserRepository users = new UserRepository(em);
            User user = users.findByEmail(email).orElse(null);
            if (user == null) {
                passwordService.matchesDummy(rawPassword);
                throw new ApiException(401, ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
            }
            if (!passwordService.matches(rawPassword, user.getPasswordHash())) {
                throw new ApiException(401, ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
            }
            if (user.isBlocked()) {
                throw new ApiException(403, ErrorCode.ACCOUNT_BLOCKED, "Account is blocked");
            }

            RefreshTokenRepository tokens = new RefreshTokenRepository(em);
            tokens.deleteExpiredByUserId(user.getId());

            String accessToken = jwtService.createAccessToken(user);
            String refreshToken = jwtService.createRefreshToken(user);
            RefreshToken row = new RefreshToken();
            row.setUser(user);
            row.setTokenHash(TokenHasher.sha256Hex(refreshToken));
            row.setExpiresAt(Instant.now().plusSeconds(config.getJwtRefreshTtlSeconds()));
            tokens.save(row);

            LoginUserDto dto = new LoginUserDto(user.getId(), user.getEmail(), user.getRole().name(),
                    user.isVerified());
            return new LoginResponse(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), dto);
        });
    }

    public RefreshResponse refresh(RefreshRequest request) {
        UUID userId;
        try {
            userId = jwtService.parseRefresh(request.refreshToken());
        } catch (InvalidTokenException e) {
            throw new ApiException(401, ErrorCode.INVALID_OR_EXPIRED_REFRESH_TOKEN,
                    "Invalid or expired refresh token");
        }
        String hash = TokenHasher.sha256Hex(request.refreshToken());

        return Tx.inRead(em -> {
            RefreshTokenRepository tokens = new RefreshTokenRepository(em);
            RefreshToken row = tokens.findByTokenHash(hash).orElse(null);
            if (row == null || row.isExpired()) {
                throw new ApiException(401, ErrorCode.INVALID_OR_EXPIRED_REFRESH_TOKEN,
                        "Invalid or expired refresh token");
            }
            User user = row.getUser();
            if (user == null || !user.getId().equals(userId) || user.isBlocked()) {
                throw new ApiException(401, ErrorCode.INVALID_OR_EXPIRED_REFRESH_TOKEN,
                        "Invalid or expired refresh token");
            }
            String accessToken = jwtService.createAccessToken(user);
            return new RefreshResponse(accessToken, jwtService.getAccessTtlSeconds());
        });
    }

    public void logout(UUID userId, LogoutRequest request) {
        String hash = TokenHasher.sha256Hex(request.refreshToken());
        Tx.inTransactionVoid(em -> new RefreshTokenRepository(em).deleteByTokenHashAndUserId(hash, userId));
    }

    private static boolean isUniqueViolation(PersistenceException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
            if (t instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }
}
