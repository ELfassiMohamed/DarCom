package com.backend.dto.auth;

import com.backend.security.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotNull @StrongPassword String newPassword) {
}
