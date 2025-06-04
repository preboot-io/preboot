package io.preboot.query;

/**
 * Contains information about a join between entities, supporting both collection joins and aggregate reference joins.
 */
public record JoinInfo(
        String targetTable, String alias, String sourceColumn, String targetColumn, boolean isAggregateReference) {
    public static JoinInfo forAggregateReference(
            String targetTable, String alias, String sourceColumn, String targetColumn) {
        return new JoinInfo(targetTable, alias, sourceColumn, targetColumn, true);
    }

    /**
     * Creates a JoinInfo for collection. For collections, the source column is in the child table (e.g.,
     * order_items.order_id) and the target column is the primary key in the parent table (e.g., orders.id)
     */
    public static JoinInfo forCollection(String targetTable, String alias, String foreignKeyColumn) {
        return new JoinInfo(targetTable, alias, foreignKeyColumn, "id", false);
    }
}
