package io.preboot.query;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.jdbc.support.SqlValue;

class ArraySqlValue implements SqlValue {
    private final Object[] arr;
    private final String dbTypeName;

    public static ArraySqlValue create(final Object[] arr) {
        return new ArraySqlValue(arr, determineDbTypeName(arr));
    }

    public static ArraySqlValue create(final Object[] arr, final String dbTypeName) {
        return new ArraySqlValue(arr, dbTypeName);
    }

    private ArraySqlValue(final Object[] arr, final String dbTypeName) {
        this.arr = arr;
        this.dbTypeName = dbTypeName;
    }

    @Override
    public void setValue(final PreparedStatement ps, final int paramIndex) throws SQLException {
        final Array arrayValue = ps.getConnection().createArrayOf(dbTypeName, arr);
        ps.setArray(paramIndex, arrayValue);
    }

    @Override
    public void cleanup() {}

    private static String determineDbTypeName(final Object[] arr) {
        if (arr == null || arr.length == 0) {
            return "varchar"; // default to varchar for empty arrays
        }

        // Get the type of the first non-null element
        Object firstNonNull =
                Arrays.stream(arr).filter(Objects::nonNull).findFirst().orElse(null);

        if (firstNonNull == null) {
            return "varchar"; // default to varchar for null arrays
        }

        // Map Java types to PostgreSQL types
        if (firstNonNull instanceof String) {
            return "varchar";
        } else if (firstNonNull instanceof Integer) {
            return "integer";
        } else if (firstNonNull instanceof Long) {
            return "bigint";
        } else if (firstNonNull instanceof Double) {
            return "double precision";
        } else if (firstNonNull instanceof Float) {
            return "real";
        } else if (firstNonNull instanceof BigDecimal) {
            return "numeric";
        } else if (firstNonNull instanceof Boolean) {
            return "boolean";
        } else if (firstNonNull instanceof LocalDateTime) {
            return "timestamp";
        } else if (firstNonNull instanceof LocalDate) {
            return "date";
        } else if (firstNonNull instanceof Enum) {
            return "varchar";
        }

        // Default to varchar if type is not recognized
        return "varchar";
    }
}
