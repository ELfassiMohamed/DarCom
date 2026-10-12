package com.backend.service;

import com.backend.config.AppConfig;
import com.backend.domain.AuthToken;
import com.backend.domain.RefreshToken;
import com.backend.domain.User;
import com.backend.domain.enums.TokenPurpose;
import com.backend.domain.enums.UserRole;
import com.backend.dto.auth.ForgotPasswordRequest;
import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.LoginResponse;
import com.backend.dto.auth.LoginUserDto;
import com.backend.dto.auth.LogoutRequest;
import com.backend.dto.auth.RefreshRequest;
import com.backend.dto.auth.RefreshResponse;
import com.backend.dto.auth.RegisterRequest;
import com.backend.dto.auth.RegisterResponse;
import com.backend.dto.auth.ResendVerificationRequest;
import com.backend.dto.auth.ResetPasswordRequest;
import com.backend.dto.auth.VerifyEmailRequest;
import com.backend.dto.user.UserDto;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import com.backend.persistence.Tx;
import com.backend.repository.AuthTokenRepository;
import com.backend.repository.RefreshTokenRepository;
import com.backend.repository.UserRepository;
import com.backend.security.InvalidTokenException;
import com.backend.security.JwtService;
import com.backend.security.PasswordService;
import com.backend.security.TokenHasher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.PersistenceException;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

/** Single place for auth side effects (verification email goes out from here after register). */
@Singleton
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    /** Max verification/reset emails per address per hour. */
    private static final int EMAIL_RATE_LIMIT_PER_HOUR = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppConfig config;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final MailService mailService;

    @Inject
    public AuthService(AppConfig config, JwtService jwtService, PasswordService passwordService,
                       MailService mailService) {
        this.config = config;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
        this.mailService = mailService;
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
                RegisterResponse created = new RegisterResponse(user.getId(), user.getEmail(), user.getFullName(),
                        user.getRole().name(), user.isVerified(), user.getCreatedAt());
                String rawToken = issueEmailToken(em, user, TokenPurpose.EMAIL_VERIFICATION,
                        Instant.now().plusSeconds(config.getVerificationTokenTtlHours() * 3600));
                sendVerificationMailBestEffort(user, rawToken);
                return created;
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

    /** POST /auth/verify-email (public) → 200 with the verified user. */
    public UserDto verifyEmail(VerifyEmailRequest request) {
        String hash = TokenHasher.sha256Hex(request.token());
        return Tx.inTransaction(em -> {
            AuthToken row = new AuthTokenRepository(em).findByTokenHash(hash).orElse(null);
            if (row == null || row.getPurpose() != TokenPurpose.EMAIL_VERIFICATION
                    || row.isUsed() || row.isExpired()) {
                throw new ApiException(400, ErrorCode.INVALID_OR_EXPIRED_VERIFICATION_TOKEN,
                        "Invalid or expired verification token");
            }
            User user = row.getUser();
            user.setVerified(true);
            row.setUsedAt(Instant.now());
            return UserDto.from(user);
        });
    }

    /** POST /auth/resend-verification (public) → always 204 (no email enumeration). */
    public void resendVerification(ResendVerificationRequest request) {
        String email = normalizeEmail(request.email());
        PendingMail pending = Tx.inTransaction(em -> {
            User user = new UserRepository(em).findByEmail(email).orElse(null);
            if (user == null || user.isVerified() || user.isBlocked()) {
                return null;
            }
            checkEmailRateLimit(em, user.getId(), TokenPurpose.EMAIL_VERIFICATION);
            String rawToken = issueEmailToken(em, user, TokenPurpose.EMAIL_VERIFICATION,
                    Instant.now().plusSeconds(config.getVerificationTokenTtlHours() * 3600));
            return new PendingMail(email, user.getFullName(), rawToken);
        });
        if (pending != null) {
            mailService.sendVerificationEmail(pending.to(), pending.fullName(), pending.rawToken());
        }
    }

    /** POST /auth/forgot-password (public) → always 204 (no email enumeration). */
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        PendingMail pending = Tx.inTransaction(em -> {
            User user = new UserRepository(em).findByEmail(email).orElse(null);
            if (user == null || user.isBlocked()) {
                return null;
            }
            checkEmailRateLimit(em, user.getId(), TokenPurpose.PASSWORD_RESET);
            String rawToken = issueEmailToken(em, user, TokenPurpose.PASSWORD_RESET,
                    Instant.now().plusSeconds(config.getPasswordResetTokenTtlMinutes() * 60));
            return new PendingMail(email, user.getFullName(), rawToken);
        });
        if (pending != null) {
            mailService.sendPasswordResetEmail(pending.to(), pending.fullName(), pending.rawToken());
        }
    }

    /** POST /auth/reset-password (public) → 204; single-use token, revokes all sessions. */
    public void resetPassword(ResetPasswordRequest request) {
        passwordService.validate(request.newPassword());
        String hash = TokenHasher.sha256Hex(request.token());
        Tx.inTransactionVoid(em -> {
            AuthToken row = new AuthTokenRepository(em).findByTokenHash(hash).orElse(null);
            if (row == null || row.getPurpose() != TokenPurpose.PASSWORD_RESET
                    || row.isUsed() || row.isExpired()) {
                throw new ApiException(400, ErrorCode.INVALID_OR_EXPIRED_RESET_TOKEN,
                        "Invalid or expired reset token");
            }
            User user = row.getUser();
            user.setPasswordHash(passwordService.hash(request.newPassword()));
            row.setUsedAt(Instant.now());
            new RefreshTokenRepository(em).deleteAllByUserId(user.getId());
            new AuthTokenRepository(em).deleteExpiredByUserAndPurpose(user.getId(), TokenPurpose.PASSWORD_RESET);
        });
    }

    /** Detached mail data: the raw token must leave the transaction, the entity must not. */
    private record PendingMail(String to, String fullName, String rawToken) {
    }

    /** Creates a fresh single-use token (invalidating previous ones of the same purpose). */    private static String issueEmailToken(jakarta.persistence.EntityManager em, User user,
                                          TokenPurpose purpose, Instant expiresAt) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthTokenRepository tokens = new AuthTokenRepository(em);
        tokens.deleteAllByUserAndPurpose(user.getId(), purpose);
        AuthToken row = new AuthToken();
        row.setUser(user);
        row.setTokenHash(TokenHasher.sha256Hex(rawToken));
        row.setPurpose(purpose);
        row.setExpiresAt(expiresAt);
        tokens.save(row);
        return rawToken;
    }

    private static void checkEmailRateLimit(jakarta.persistence.EntityManager em, UUID userId,
                                            TokenPurpose purpose) {
        long recent = new AuthTokenRepository(em)
                .countRecentByUserAndPurpose(userId, purpose, Instant.now().minusSeconds(3600));
        if (recent >= EMAIL_RATE_LIMIT_PER_HOUR) {
            throw new ApiException(429, ErrorCode.TOO_MANY_REQUESTS,
                    "Too many emails requested. Please try again later");
        }
    }

    /** Registration must not fail just because SMTP is down; the resend endpoint covers that. */
    private void sendVerificationMailBestEffort(User user, String rawToken) {
        try {
            mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), rawToken);
        } catch (RuntimeException e) {
            LOG.warning("Failed to send verification email to " + user.getEmail() + ": " + e.getMessage());
        }
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
