package com.backend.dto.auth;

import com.backend.security.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull @StrongPassword String password,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Pattern(regexp = "HOST|VISITOR") String role) {
}
