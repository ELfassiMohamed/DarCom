package com.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockUserRequest(
        @NotBlank @Size(max = 500) String reason) {
}
