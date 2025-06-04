package io.preboot.query;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class CompoundExpression implements CriteriaExpression {
    private final List<CriteriaExpression> expressions;
    private final LogicalOperator operator;

    public CompoundExpression(List<CriteriaExpression> expressions, LogicalOperator operator) {
        this.expressions = expressions;
        this.operator = operator;
    }

    @Override
    public String toSql(SqlContext context) {
        if (expressions.isEmpty()) {
            return "1=1"; // Always true for empty conditions
        }

        List<String> conditions =
                expressions.stream().map(expr -> expr.toSql(context)).collect(Collectors.toList());

        return conditions.size() == 1
                ? conditions.get(0)
                : "(" + String.join(" " + operator.sql() + " ", conditions) + ")";
    }

    @Override
    public void addParameters(SqlParameterSource paramSource) {
        expressions.forEach(expr -> expr.addParameters(paramSource));
    }
}
