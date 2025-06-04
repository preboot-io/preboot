package io.preboot.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
    // Optional - can add a priority if you want to control execution order, higher number means higher priority
    int priority() default 0;

    /**
     * Optional type parameter filter for generic events. When set, the handler will only receive events where the
     * generic type parameter matches this class. For example, for events like Event<T>, this filters based on the
     * actual type of T.
     */
    Class<?> typeParameter() default void.class;
}
