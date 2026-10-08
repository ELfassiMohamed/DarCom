package com.backend.exception;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String errorCode, String message) {
        super(403, errorCode, message);
    }
}
