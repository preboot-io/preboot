package io.preboot.core.json;

public class JsonParsingException extends RuntimeException {
    JsonParsingException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
