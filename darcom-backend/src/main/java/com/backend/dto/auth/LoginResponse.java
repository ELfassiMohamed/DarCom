package com.backend.dto.auth;

public record LoginResponse(String accessToken, String refreshToken, long expiresIn, LoginUserDto user) {
}
