package com.backend.exception;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String errorCode, String message) {
        super(401, errorCode, message);
    }
}
