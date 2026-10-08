package com.backend.dto.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects ADMIN at the validation boundary (→ 400 VALIDATION_ERROR, spec-literal
 * for "role not HOST/VISITOR"). The service's INVALID_ROLE 403 stays as backstop
 * for direct callers — same two-layer shape as booking dates.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotAdminValidator.class)
@Documented
public @interface NotAdmin {
    String message() default "role must be HOST or VISITOR";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
