package com.backend.dto.auth;

import com.backend.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    /** Max 72 — bcrypt only looks at the first 72 bytes anyway (Task 04 §5.1); past that point a longer password buys nothing, so this is the actual boundary, not an arbitrary one. */
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String fullName;

    private String phone;

    /**
     * Deliberately no restriction here beyond "present." AuthService.register
     * (Task 04) already rejects ADMIN — a privilege-escalation guard on a
     * public endpoint, kept in the service specifically so a DTO annotation
     * isn't the only thing standing in the way (Task 04 §7's note on this).
     */
    @NotNull
    @NotAdmin
    private UserRole role;

    public RegisterRequest() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
