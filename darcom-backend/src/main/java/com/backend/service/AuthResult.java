package com.backend.service;

import com.backend.domain.User;

public record AuthResult(String accessToken, String refreshToken, User user) {
}
