package com.backend.service;

/** POST /auth/refresh response, spec-literal: fresh access token + TTL only — the presented refresh token stays valid (no rotation). */
public record RefreshResult(String accessToken, long expiresIn) {
}
