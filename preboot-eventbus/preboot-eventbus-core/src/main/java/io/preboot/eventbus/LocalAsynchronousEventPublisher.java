package io.preboot.eventbus;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalAsynchronousEventPublisher implements AsynchronousEventPublisher {

    private final LocalEventHandlerRepository localEventHandlerRepository;
    private final Executor executor;

    public LocalAsynchronousEventPublisher(LocalEventHandlerRepository localEventHandlerRepository, Executor executor) {
        this.localEventHandlerRepository = localEventHandlerRepository;
        this.executor = executor;
    }

    @Override
    public <T> void publish(final T event) {
        if (localEventHandlerRepository.isHandlerMissing(event)) {
            if (event.getClass().getAnnotation(ExceptionIfNoHandler.class) != null) {
                throw new NoEventHandlerException(event);
            } else {
                log.warn("No handler found for event: {}", event);
                return;
            }
        }
        if (executor != null) {
            executor.execute(() -> localEventHandlerRepository.publish(event));
            return;
        }
        throw new IllegalStateException("Executor implementation is not provided");
    }
}
