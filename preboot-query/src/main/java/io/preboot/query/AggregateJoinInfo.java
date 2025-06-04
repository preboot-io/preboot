package io.preboot.query;

public record AggregateJoinInfo(String targetTable, String alias, String sourceColumn, String targetColumn) {}
