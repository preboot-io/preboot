package io.preboot.auth.api.exception;

public class TenantAccessDeniedException extends RuntimeException {
    public TenantAccessDeniedException(final String message) {
        super(message);
    }
}
