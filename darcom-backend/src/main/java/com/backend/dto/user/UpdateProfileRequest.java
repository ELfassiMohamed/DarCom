package com.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 255) String fullName,
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$") String phone) {
}
