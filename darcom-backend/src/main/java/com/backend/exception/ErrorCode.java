package com.backend.exception;

/** Machine-readable error codes used in the {@code error} field of the error envelope. */
public final class ErrorCode {

    private ErrorCode() {
    }

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String ERROR = "ERROR";

    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_OR_EXPIRED_REFRESH_TOKEN = "INVALID_OR_EXPIRED_REFRESH_TOKEN";
    public static final String ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ALREADY_BLOCKED = "ALREADY_BLOCKED";
    public static final String NOT_BLOCKED = "NOT_BLOCKED";
    public static final String CANNOT_BLOCK_ADMIN = "CANNOT_BLOCK_ADMIN";
    public static final String INVALID_OR_EXPIRED_VERIFICATION_TOKEN = "INVALID_OR_EXPIRED_VERIFICATION_TOKEN";
    public static final String INVALID_OR_EXPIRED_RESET_TOKEN = "INVALID_OR_EXPIRED_RESET_TOKEN";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
    public static final String LISTING_NOT_FOUND = "LISTING_NOT_FOUND";
    public static final String NOT_OWNER = "NOT_OWNER";
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String CANNOT_DELETE_WITH_ACTIVE_BOOKINGS = "CANNOT_DELETE_WITH_ACTIVE_BOOKINGS";
    public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
}
