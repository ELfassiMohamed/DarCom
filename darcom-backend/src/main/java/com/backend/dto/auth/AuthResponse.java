package com.backend.dto.auth;

import com.backend.security.JwtService;
import com.backend.service.AuthResult;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.accessToken(), result.refreshToken(),
                JwtService.ACCESS_TOKEN_TTL.getSeconds(), UserResponse.from(result.user()));
    }
}
