package com.backend.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Bean-validation constraint delegating to {@link PasswordService#policyError(String)}. */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface StrongPassword {

    String message() default "Password does not meet the password policy";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
