package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Shared by /auth/refresh and /auth/logout — both need exactly one field. */
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
