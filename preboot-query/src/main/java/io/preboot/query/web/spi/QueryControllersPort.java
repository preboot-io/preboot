package io.preboot.query.web.spi;

/**
 * Service provider interface for query controllers that need access to security context and event publishing
 * capabilities. This interface provides a unified port for controllers to interact with the application's security and
 * event systems without depending on specific implementations.
 *
 * <p>Implementations should be provided by the application using the query module, typically by integrating with their
 * security framework and event bus.
 */
public interface QueryControllersPort {

    /**
     * Gets the current user context containing user and tenant information.
     *
     * @return Current user context, or null if no user is authenticated
     */
    UserContext getUserContext();

    /**
     * Publishes an event to the event system.
     *
     * @param event The event to publish
     * @param <T> The type of the event
     */
    <T> void publishEvent(T event);
}
