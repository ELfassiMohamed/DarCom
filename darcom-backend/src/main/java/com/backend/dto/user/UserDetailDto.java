package com.backend.dto.user;

import com.backend.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserDetailDto(UUID id, String email, String fullName, String phone, String role, boolean verified,
        boolean blocked, Instant createdAt, long listingCount, long reportCount) {

    public static UserDetailDto from(User user, long listingCount, long reportCount) {
        return new UserDetailDto(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getRole().name(), user.isVerified(), user.isBlocked(), user.getCreatedAt(),
                listingCount, reportCount);
    }
}
