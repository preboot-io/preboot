package io.preboot.query.exception;

public class InvalidFilterCriteriaException extends FilteringException {
    private final String field;
    private final String operation;

    public InvalidFilterCriteriaException(String field, String operation, String message) {
        super(String.format(
                "Invalid filter criteria for field '%s' with operation '%s': %s", field, operation, message));
        this.field = field;
        this.operation = operation;
    }

    public String getField() {
        return field;
    }

    public String getOperation() {
        return operation;
    }
}
