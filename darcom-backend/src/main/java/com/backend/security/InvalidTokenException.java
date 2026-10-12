package com.backend.security;

/** Internal signal for any rejected token (bad signature, expired, wrong issuer or wrong {@code typ}). */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
