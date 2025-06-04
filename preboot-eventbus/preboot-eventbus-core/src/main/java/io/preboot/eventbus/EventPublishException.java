package io.preboot.eventbus;

public class EventPublishException extends RuntimeException {
    public EventPublishException(final Throwable cause) {
        super(cause);
    }
}
