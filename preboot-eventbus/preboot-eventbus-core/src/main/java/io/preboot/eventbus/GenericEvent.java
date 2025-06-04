package io.preboot.eventbus;

/**
 * Marker interface for events that have a generic type parameter. Implementations must provide access to the actual
 * type parameter instance to enable runtime type filtering.
 */
public interface GenericEvent<T> {
    /**
     * Gets the instance that represents the generic type parameter. This is used for runtime type filtering in event
     * handlers.
     */
    T getTypeParameter();
}
