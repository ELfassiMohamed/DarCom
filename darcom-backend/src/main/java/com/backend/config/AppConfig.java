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
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String smtpFrom;
    private final String appPublicUrl;
    private final long verificationTokenTtlHours;
    private final long passwordResetTokenTtlMinutes;
    private final String uploadDir;

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
        this.smtpHost = orDefault(firstNonBlank(System.getenv("SMTP_HOST"), dotenv.get("SMTP_HOST")),
                "smtp.gmail.com");
        this.smtpPort = (int) parseLong(firstNonBlank(System.getenv("SMTP_PORT"), dotenv.get("SMTP_PORT")), 465);
        this.smtpUsername = firstNonBlank(System.getenv("SMTP_USERNAME"), dotenv.get("SMTP_USERNAME"));
        this.smtpPassword = firstNonBlank(System.getenv("SMTP_PASSWORD"), dotenv.get("SMTP_PASSWORD"));
        String from = firstNonBlank(System.getenv("SMTP_FROM"), dotenv.get("SMTP_FROM"));
        this.smtpFrom = from != null ? from : smtpUsername;
        this.appPublicUrl = orDefault(
                firstNonBlank(System.getenv("APP_PUBLIC_URL"), dotenv.get("APP_PUBLIC_URL")),
                "http://localhost:3000");
        this.verificationTokenTtlHours = parseLong(
                firstNonBlank(System.getenv("VERIFICATION_TOKEN_TTL_HOURS"),
                        dotenv.get("VERIFICATION_TOKEN_TTL_HOURS")),
                24);
        this.passwordResetTokenTtlMinutes = parseLong(
                firstNonBlank(System.getenv("PASSWORD_RESET_TOKEN_TTL_MINUTES"),
                        dotenv.get("PASSWORD_RESET_TOKEN_TTL_MINUTES")),
                60);
        this.uploadDir = orDefault(firstNonBlank(System.getenv("UPLOAD_DIR"), dotenv.get("UPLOAD_DIR")), "uploads/photos");
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

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public String getSmtpFrom() {
        return smtpFrom;
    }

    public String getAppPublicUrl() {
        return appPublicUrl;
    }

    public long getVerificationTokenTtlHours() {
        return verificationTokenTtlHours;
    }

    public long getPasswordResetTokenTtlMinutes() {
        return passwordResetTokenTtlMinutes;
    }

    public String getUploadDir() {
        return uploadDir;
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
