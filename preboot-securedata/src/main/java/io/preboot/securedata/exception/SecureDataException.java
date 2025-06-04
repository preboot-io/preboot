package io.preboot.securedata.exception;

public class SecureDataException extends RuntimeException {
    public SecureDataException(String message) {
        super(message);
    }

    public SecureDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
