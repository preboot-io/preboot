package io.preboot.templates.api;

public class MissingTemplateKeyException extends RuntimeException {
    public MissingTemplateKeyException(String message) {
        super(message);
    }
}
