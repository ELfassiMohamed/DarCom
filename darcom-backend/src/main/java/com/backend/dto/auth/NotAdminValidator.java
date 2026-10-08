package com.backend.dto.auth;

import com.backend.domain.enums.UserRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotAdminValidator implements ConstraintValidator<NotAdmin, UserRole> {

    @Override
    public boolean isValid(UserRole value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNull owns the null case
        }
        return value != UserRole.ADMIN;
    }
}
