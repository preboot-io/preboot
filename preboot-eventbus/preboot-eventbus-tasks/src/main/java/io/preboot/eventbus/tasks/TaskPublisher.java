package io.preboot.eventbus.tasks;

public interface TaskPublisher {
    <T> void publishTask(T task);

    <T> void publishTask(final T task, String hash);
}
