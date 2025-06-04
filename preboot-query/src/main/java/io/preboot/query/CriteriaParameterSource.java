package io.preboot.query;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class CriteriaParameterSource extends MapSqlParameterSource {
    @Override
    public MapSqlParameterSource addValue(String paramName, Object value) {
        if (value instanceof Object[] arr) {
            return super.addValue(paramName, ArraySqlValue.create(arr));
        }
        return super.addValue(paramName, value);
    }
}
