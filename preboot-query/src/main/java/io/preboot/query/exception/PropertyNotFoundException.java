package io.preboot.query.exception;

public class PropertyNotFoundException extends FilteringException {
    private final String propertyPath;

    public PropertyNotFoundException(String propertyPath) {
        super(String.format("Property not found: %s", propertyPath));
        this.propertyPath = propertyPath;
    }

    public String getPropertyPath() {
        return propertyPath;
    }
}
