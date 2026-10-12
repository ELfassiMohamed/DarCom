package com.backend.dto.auth;

public record RefreshResponse(String accessToken, long expiresIn) {
}
