package com.backend.exception;

/**
 * Base for every exception a service throws to signal a specific,
 * spec-defined error condition. Carries exactly what a future JAX-RS
 * exception mapper needs to build the error envelope from
 * API-Specification.md §1 (status, error, message) with no translation
 * logic of its own.
 */
public abstract class ApiException extends RuntimeException {

    private final int status;
    private final String errorCode;

    protected ApiException(int status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
