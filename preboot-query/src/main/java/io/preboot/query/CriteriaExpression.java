package io.preboot.query;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public interface CriteriaExpression {
    String toSql(SqlContext context);

    void addParameters(SqlParameterSource paramSource);
}
