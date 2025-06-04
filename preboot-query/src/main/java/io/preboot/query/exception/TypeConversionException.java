package io.preboot.query.exception;

public class TypeConversionException extends FilteringException {
    private final Class<?> sourceType;
    private final Class<?> targetType;

    public TypeConversionException(Class<?> sourceType, Class<?> targetType, String message) {
        super(String.format(
                "Failed to convert from %s to %s: %s",
                sourceType.getSimpleName(), targetType.getSimpleName(), message));
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public Class<?> getTargetType() {
        return targetType;
    }
}
