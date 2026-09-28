package com.backend.exception;

public class ConflictException extends ApiException {
    public ConflictException(String errorCode, String message) {
        super(409, errorCode, message);
    }
}
