package com.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class RefreshTokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RefreshTokenHasher() {
    }

    /** A new, high-entropy raw refresh token — what goes to the client. Never stored as-is. */
    public static String generateRaw() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of a raw token, hex-encoded — this is what RefreshToken.tokenHash actually stores. */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory algorithm on every JVM per the JCA spec — this never actually fires.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
