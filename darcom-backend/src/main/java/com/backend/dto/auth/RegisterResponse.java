package com.backend.dto.auth;

import com.backend.domain.User;
import com.backend.domain.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * POST /auth/register response, spec-exact: no phone, no blocked flag
 * (the spec example carries neither — extra fields stay out).
 */
public record RegisterResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        boolean verified,
        Instant createdAt
) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isVerified(),
                user.getCreatedAt()
        );
    }
}
