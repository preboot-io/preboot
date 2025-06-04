package io.preboot.query;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchCriteria {
    private String field;
    private String operation;
    private Object value;
    private String joinTable;
    private String joinColumn;
    private final String parameterName;
    private List<SearchCriteria> children;
    private LogicalOperator logicalOperator;
    private boolean orPredicate;

    // Public constructor without parameterName for external use
    public SearchCriteria(String field, String operation, Object value, boolean orPredicate) {
        this(field, operation, value, orPredicate, null, null, null);
    }

    // Package-private constructor with parameterName for internal use
    SearchCriteria(
            String field,
            String operation,
            Object value,
            boolean orPredicate,
            String joinTable,
            String joinColumn,
            String parameterName) {
        this.field = field;
        this.operation = operation;
        this.value = value;
        this.orPredicate = orPredicate;
        this.joinTable = joinTable;
        this.joinColumn = joinColumn;
        this.parameterName = parameterName;
    }

    // Constructor for compound criteria
    SearchCriteria(List<SearchCriteria> children, LogicalOperator logicalOperator, String parameterName) {
        this.children = children;
        this.logicalOperator = logicalOperator;
        this.parameterName = parameterName;
    }

    public boolean isCompound() {
        return children != null && !children.isEmpty();
    }
}
