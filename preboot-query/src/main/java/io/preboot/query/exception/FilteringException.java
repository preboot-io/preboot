package io.preboot.query.exception;

public class FilteringException extends RuntimeException {
    public FilteringException(String message) {
        super(message);
    }

    public FilteringException(String message, Throwable cause) {
        super(message, cause);
    }
}
