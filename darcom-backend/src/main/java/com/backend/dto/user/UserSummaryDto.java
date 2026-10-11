package com.backend.dto.user;

import com.backend.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryDto(UUID id, String email, String fullName, String role, boolean verified,
        boolean blocked, Instant createdAt) {

    public static UserSummaryDto from(User user) {
        return new UserSummaryDto(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.isVerified(), user.isBlocked(), user.getCreatedAt());
    }
}
