package io.preboot.query;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {
    private String field;
    private String operator;
    private Object value;
    private List<FilterCriteria> children;
    private LogicalOperator logicalOperator;

    public Object getValue() {
        if (isCompound()) {
            return null;
        }
        if ("like".equals(operator)) {
            return value + "%";
        }
        return value;
    }

    public boolean isCompound() {
        return children != null && !children.isEmpty();
    }

    public boolean isNullOperation() {
        return "isnull".equals(operator) || "isnotnull".equals(operator);
    }

    public CriteriaExpression toExpression(AtomicInteger paramCounter) {
        if (isCompound()) {
            return new CompoundExpression(
                    children.stream()
                            .map(child -> child.toExpression(paramCounter))
                            .collect(Collectors.toList()),
                    logicalOperator);
        } else {
            // For NULL operations, we don't need to increment the counter
            // since we don't use parameters
            String paramName =
                    isNullOperation() ? null : field.replace(".", "_") + "_" + paramCounter.getAndIncrement();
            return new SimpleExpression(field, operator, getValue(), paramName);
        }
    }

    // Equals
    public static FilterCriteria eq(String field, Object value) {
        return FilterCriteria.builder().field(field).operator("eq").value(value).build();
    }

    // Not equals
    public static FilterCriteria neq(String field, Object value) {
        return FilterCriteria.builder()
                .field(field)
                .operator("neq")
                .value(value)
                .build();
    }

    // equals (case-insensitive)
    public static FilterCriteria eqic(String field, String value) {
        return FilterCriteria.builder()
                .field(field)
                .operator("eqic")
                .value(value)
                .build();
    }

    // Like (case-insensitive)
    public static FilterCriteria like(String field, String value) {
        return FilterCriteria.builder()
                .field(field)
                .operator("like")
                .value(value)
                .build();
    }

    // Greater than
    public static FilterCriteria gt(String field, Object value) {
        return FilterCriteria.builder().field(field).operator("gt").value(value).build();
    }

    // Less than
    public static FilterCriteria lt(String field, Object value) {
        return FilterCriteria.builder().field(field).operator("lt").value(value).build();
    }

    // Greater than or equal to
    public static FilterCriteria gte(String field, Object value) {
        return FilterCriteria.builder()
                .field(field)
                .operator("gte")
                .value(value)
                .build();
    }

    // Less than or equal to
    public static FilterCriteria lte(String field, Object value) {
        return FilterCriteria.builder()
                .field(field)
                .operator("lte")
                .value(value)
                .build();
    }

    // Between
    public static FilterCriteria between(String field, Object from, Object to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both 'from' and 'to' values must be provided for BETWEEN operator");
        }
        return FilterCriteria.builder()
                .field(field)
                .operator("between")
                .value(new Object[] {from, to})
                .build();
    }

    // IN
    public static FilterCriteria in(String field, Object... values) {
        return FilterCriteria.builder()
                .field(field)
                .operator("in")
                .value(values)
                .build();
    }

    // ARRAY OVERLAP
    public static FilterCriteria ao(String field, Object... values) {
        return FilterCriteria.builder()
                .field(field)
                .operator("ao")
                .value(values)
                .build();
    }

    // IS NULL
    public static FilterCriteria isNull(String field) {
        return FilterCriteria.builder()
                .field(field)
                .operator("isnull")
                .value(null)
                .build();
    }

    // IS NOT NULL
    public static FilterCriteria isNotNull(String field) {
        return FilterCriteria.builder()
                .field(field)
                .operator("isnotnull")
                .value(null)
                .build();
    }

    // Helper method for OR criteria
    public static FilterCriteria or(List<FilterCriteria> criteria) {
        return FilterCriteria.builder()
                .children(criteria)
                .logicalOperator(LogicalOperator.OR)
                .build();
    }

    // Helper method for AND criteria
    public static FilterCriteria and(List<FilterCriteria> criteria) {
        return FilterCriteria.builder()
                .children(criteria)
                .logicalOperator(LogicalOperator.AND)
                .build();
    }
}
