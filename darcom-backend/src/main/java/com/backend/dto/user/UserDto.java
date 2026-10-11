package com.backend.dto.user;

import com.backend.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String email, String fullName, String phone, String role, boolean verified,
        boolean blocked, Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getRole().name(), user.isVerified(), user.isBlocked(), user.getCreatedAt());
    }
}
