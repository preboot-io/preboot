package io.preboot.query;

import io.preboot.query.exception.InvalidFilterCriteriaException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class SpecificationBuilder {
    public enum Operator {
        EQUALS("eq", "="),
        NOT_EQUALS("neq", "!="),
        EQUALS_IGNORE_CASE("eqic", "eqic"),
        LIKE("like", "ILIKE"),
        GREATER_THAN("gt", ">"),
        LESS_THAN("lt", "<"),
        GREATER_THAN_EQUALS("gte", ">="),
        LESS_THAN_EQUALS("lte", "<="),
        BETWEEN("between", "BETWEEN"),
        IN("in", "IN"),
        ARRAY_OVERLAP("ao", "&& ARRAY"),
        IS_NULL("isnull", "IS NULL"),
        IS_NOT_NULL("isnotnull", "IS NOT NULL");

        private final String apiOperator;
        private final String sqlOperator;

        Operator(final String apiOperator, final String sqlOperator) {
            this.apiOperator = apiOperator;
            this.sqlOperator = sqlOperator;
        }

        public static Operator fromApiOperator(String apiOperator) {
            for (Operator op : values()) {
                if (op.apiOperator.equals(apiOperator)) {
                    return op;
                }
            }
            throw new InvalidFilterCriteriaException("unknown", apiOperator, "Unsupported operator: " + apiOperator);
        }

        public String getSqlOperator() {
            return sqlOperator;
        }
    }

    public <T> JdbcSpecification<T> buildSpecification(Class<T> entityClass, List<FilterCriteria> params) {
        Assert.notNull(entityClass, "Entity class must not be null");
        Assert.notNull(params, "Parameters must not be null");

        List<FilterCriteria> processedCriteria = new ArrayList<>();

        for (int i = 0; i < params.size(); i++) {
            FilterCriteria criteria = params.get(i);
            if (criteria.isCompound()) {
                List<FilterCriteria> childCriteria = new ArrayList<>();

                for (int j = 0; j < criteria.getChildren().size(); j++) {
                    FilterCriteria child = criteria.getChildren().get(j);
                    if (!isSpecialParameter(child.getField())) {
                        // Convert operator and create new FilterCriteria
                        childCriteria.add(FilterCriteria.builder()
                                .field(child.getField())
                                .operator(fromApiOperator(child.getOperator()).getSqlOperator())
                                .value(child.getValue())
                                .build());
                    }
                }

                if (!childCriteria.isEmpty()) {
                    processedCriteria.add(FilterCriteria.builder()
                            .children(childCriteria)
                            .logicalOperator(criteria.getLogicalOperator())
                            .build());
                }
            } else if (!isSpecialParameter(criteria.getField())) {
                processedCriteria.add(FilterCriteria.builder()
                        .field(criteria.getField())
                        .operator(fromApiOperator(criteria.getOperator()).getSqlOperator())
                        .value(criteria.getValue())
                        .build());
            }
        }

        JdbcSpecification<T> spec = new JdbcSpecification<>();
        spec.withCriteria(processedCriteria);
        return spec;
    }

    private Operator fromApiOperator(String apiOperator) {
        return Operator.fromApiOperator(apiOperator);
    }

    private boolean isSpecialParameter(String paramName) {
        if (paramName == null) {
            return false;
        }
        return paramName.startsWith("page") || paramName.startsWith("size") || paramName.startsWith("sort");
    }
}
