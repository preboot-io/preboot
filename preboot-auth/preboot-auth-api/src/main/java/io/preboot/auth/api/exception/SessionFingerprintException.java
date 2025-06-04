package io.preboot.auth.api.exception;

public class SessionFingerprintException extends RuntimeException {
    public SessionFingerprintException(final String message) {
        super(message);
    }
}
