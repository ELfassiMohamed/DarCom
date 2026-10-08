package com.backend.exception;

public class NotFoundException extends ApiException {
    public NotFoundException(String errorCode, String message) {
        super(404, errorCode, message);
    }
}
