package io.preboot.eventbus;

public class NoEventHandlerException extends RuntimeException {
    public <T> NoEventHandlerException(final T event) {
        super("No event handler found for event: " + event);
    }
}
