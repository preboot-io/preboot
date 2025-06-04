package io.preboot.auth.api.exception;

public class TenantInactiveException extends RuntimeException {
    public TenantInactiveException(String message) {
        super(message);
    }
}
