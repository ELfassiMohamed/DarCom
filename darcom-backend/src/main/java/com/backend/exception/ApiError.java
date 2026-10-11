package com.backend.exception;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Error envelope: exactly the spec shape {@code {timestamp, status, error, message, path}}. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now().truncatedTo(ChronoUnit.SECONDS), status, error, message, path);
    }
}
