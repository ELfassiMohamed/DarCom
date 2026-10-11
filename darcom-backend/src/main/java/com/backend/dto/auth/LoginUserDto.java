package com.backend.dto.auth;

import java.util.UUID;

public record LoginUserDto(UUID id, String email, String role, boolean verified) {
}
