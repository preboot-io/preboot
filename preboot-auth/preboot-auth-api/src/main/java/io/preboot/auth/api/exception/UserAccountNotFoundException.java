package io.preboot.auth.api.exception;

public class UserAccountNotFoundException extends RuntimeException {
    public UserAccountNotFoundException(final String message) {
        super(message);
    }
}
