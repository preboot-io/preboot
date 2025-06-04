package io.preboot.auth.api.exception;

public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException(final String message) {
        super(message);
    }
}
