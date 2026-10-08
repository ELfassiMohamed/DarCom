package com.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT /users/me body. Email/password deliberately absent (spec §5). */
public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 30)
    private String phone;

    public UpdateProfileRequest() {
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
