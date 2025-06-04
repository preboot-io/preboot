package io.preboot.query;

public enum LogicalOperator {
    AND("AND"),
    OR("OR");

    private final String sql;

    LogicalOperator(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
