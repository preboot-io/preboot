package io.preboot.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method within a bean as an event handler. Methods annotated with {@code @EventHandler} will be registered to
 * receive events that match the type of their first parameter. The event dispatching mechanism will look for public
 * methods with this annotation and a single parameter (the event object).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {

    /**
     * Defines the priority of the event handler. Handlers with higher priority values (e.g., 100) are generally
     * executed before handlers with lower priority values (e.g., 1). The default priority is 0. The exact behavior of
     * priority-based ordering can depend on the event bus implementation.
     *
     * @return the priority of the event handler.
     */
    int priority() default 0;

    /**
     * Optional type parameter filter for generic events. When set, the handler will only receive events where the
     * generic type parameter of the event (if the event is a {@link GenericEvent} or similar construct) matches this
     * specified class. For example, for events like {@literal Event<String>} or {@literal Event<Integer>}, this setting
     * allows the handler to specify it's only interested in {@code String.class} as the generic type.
     *
     * <p>If the event is not generic or if this is set to {@code void.class} (the default), this filter is not applied
     * based on the generic type parameter.
     *
     * @return the class of the generic type parameter to filter on, or {@code void.class} if no specific filtering by
     *     the generic type is needed.
     */
    Class<?> typeParameter() default void.class;
}
