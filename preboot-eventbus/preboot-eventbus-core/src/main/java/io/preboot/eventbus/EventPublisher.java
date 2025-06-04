package io.preboot.eventbus;

public interface EventPublisher {
    <T> void publish(T event);
}
