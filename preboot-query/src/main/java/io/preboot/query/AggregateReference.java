package io.preboot.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a reference to another aggregate root. This is used to establish relationships between aggregates
 * while maintaining aggregate boundaries.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateReference {
    /** The target entity class */
    Class<?> target();

    /** The column in the target table containing the UUID */
    String targetColumn() default "uuid";

    /** The column in the source table containing the reference UUID */
    String sourceColumn() default "";

    /** Alias to be used in queries when referencing this aggregate */
    String alias();
}
