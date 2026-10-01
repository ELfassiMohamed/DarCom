package com.backend.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordHasher {

    /** 2^12 rounds — a reasonable default cost factor as of 2026; revisit upward as hardware improves. */
    private static final int COST = 12;

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    public static boolean matches(String rawPassword, String hash) {
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), hash).verified;
    }
}
