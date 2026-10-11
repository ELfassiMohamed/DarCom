package com.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Application configuration. Reads {@code System.getenv()} first, then {@code .env}
 * (ignored if missing). Fails fast if {@code JWT_SECRET} is missing or shorter than 32 bytes.
 */
@Singleton
public class AppConfig {

    private final String jwtSecret;
    private final String jwtIssuer;
    private final long jwtAccessTtlSeconds;
    private final long jwtRefreshTtlSeconds;
    private final List<String> corsAllowedOrigins;

    public AppConfig() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.jwtSecret = firstNonBlank(System.getenv("JWT_SECRET"), dotenv.get("JWT_SECRET"));
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and be at least 32 bytes (generate with: openssl rand -base64 48)");
        }
        this.jwtIssuer = orDefault(firstNonBlank(System.getenv("JWT_ISSUER"), dotenv.get("JWT_ISSUER")), "darcom");
        this.jwtAccessTtlSeconds = parseLong(
                firstNonBlank(System.getenv("JWT_ACCESS_TTL_SECONDS"), dotenv.get("JWT_ACCESS_TTL_SECONDS")), 900);
        this.jwtRefreshTtlSeconds = parseLong(
                firstNonBlank(System.getenv("JWT_REFRESH_TTL_SECONDS"), dotenv.get("JWT_REFRESH_TTL_SECONDS")), 604800);
        String origins = orDefault(
                firstNonBlank(System.getenv("CORS_ALLOWED_ORIGINS"), dotenv.get("CORS_ALLOWED_ORIGINS")),
                "http://localhost:3000,http://localhost:5173");
        this.corsAllowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public long getJwtAccessTtlSeconds() {
        return jwtAccessTtlSeconds;
    }

    public long getJwtRefreshTtlSeconds() {
        return jwtRefreshTtlSeconds;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private static String orDefault(String value, String def) {
        return value != null ? value : def;
    }

    private static long parseLong(String value, long def) {
        if (value == null) return def;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
