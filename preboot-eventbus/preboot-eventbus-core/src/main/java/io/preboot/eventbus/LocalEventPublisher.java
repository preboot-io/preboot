package io.preboot.eventbus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalEventPublisher implements EventPublisher {

    private final LocalEventHandlerRepository localEventHandlerRepository;

    public LocalEventPublisher(LocalEventHandlerRepository localEventHandlerRepository) {
        this.localEventHandlerRepository = localEventHandlerRepository;
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
        localEventHandlerRepository.publish(event);
    }
}
