package com.backend.dto.auth;

import com.backend.service.AuthResult;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.accessToken(), result.refreshToken(), UserResponse.from(result.user()));
    }
}
