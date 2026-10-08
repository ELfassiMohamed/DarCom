package com.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * No @Email here, deliberately, matching Task 04 §3.4's reasoning one layer
 * further: every login failure — malformed email, unknown email, wrong
 * password — should funnel through the identical 401, not short-circuit to
 * a 400 for one specific kind of bad input. @NotBlank only checks something
 * was sent at all.
 */
public class LoginRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    public LoginRequest() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
