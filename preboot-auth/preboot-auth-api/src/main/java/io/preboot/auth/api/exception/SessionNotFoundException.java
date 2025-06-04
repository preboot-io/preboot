package io.preboot.auth.api.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(final String message) {
        super(message);
    }
}
