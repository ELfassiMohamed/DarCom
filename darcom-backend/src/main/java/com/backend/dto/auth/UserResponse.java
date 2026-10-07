package com.backend.dto.auth;

import com.backend.domain.User;
import com.backend.domain.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

/** No passwordHash field. That's the entire mechanism — from() only copies what's listed here. */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        UserRole role,
        boolean verified,
        boolean blocked,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.isVerified(),
                user.isBlocked(),
                user.getCreatedAt()
        );
    }
}
