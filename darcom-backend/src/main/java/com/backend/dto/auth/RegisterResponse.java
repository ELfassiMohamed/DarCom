package com.backend.dto.auth;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(UUID id, String email, String fullName, String role, boolean verified,
        Instant createdAt) {
}
