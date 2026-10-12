package com.backend.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.backend.exception.ApiException;
import com.backend.exception.ErrorCode;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;

/**
 * BCrypt passwords (cost 12) plus the password policy:
 * 8–72 bytes (UTF-8; 72 is bcrypt's hard limit), at least one lowercase,
 * one uppercase, one digit and one special character.
 */
@Singleton
public class PasswordService {

    private static final int COST = 12;
    private static final int MIN_BYTES = 8;
    private static final int MAX_BYTES = 72;

    /** Pre-computed hash for dummy verifies on unknown emails (timing-attack mitigation). */
    private static final String DUMMY_HASH = BCrypt.withDefaults().hashToString(COST, "DummyPassword0!".toCharArray());

    public String hash(String raw) {
        if (raw == null) throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Password must not be null");
        return BCrypt.withDefaults().hashToString(COST, raw.toCharArray());
    }

    public String hash(char[] raw) {
        if (raw == null) throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Password must not be null");
        return BCrypt.withDefaults().hashToString(COST, raw);
    }

    public boolean matches(String raw, String hash) {
        if (raw == null || hash == null) return false;
        return BCrypt.verifyer().verify(raw.toCharArray(), hash).verified;
    }

    public boolean matches(char[] raw, String hash) {
        if (raw == null || hash == null) return false;
        return BCrypt.verifyer().verify(raw, hash).verified;
    }

    /** Runs a BCrypt verify against a constant hash so unknown emails cost the same time. Always false. */
    public boolean matchesDummy(String raw) {
        if (raw == null) raw = "";
        BCrypt.verifyer().verify(raw.toCharArray(), DUMMY_HASH);
        return false;
    }

    /** Throws {@code ApiException(400, VALIDATION_ERROR, …)} with a specific message on violation. */
    public void validate(String password) {
        String error = policyError(password);
        if (error != null) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, error);
        }
    }

    /**
     * Returns the policy violation message, or {@code null} when the password is acceptable.
     * {@code null} passwords are considered valid here — {@code @NotNull} on the DTO reports those.
     */
    public static String policyError(String password) {
        if (password == null) return null;
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < MIN_BYTES || bytes > MAX_BYTES) {
            return "Password must be 8-72 bytes long";
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            return "Password must contain at least one digit";
        }
        if (password.chars().noneMatch(c -> !Character.isLetterOrDigit(c))) {
            return "Password must contain at least one special character";
        }
        return null;
    }
}
