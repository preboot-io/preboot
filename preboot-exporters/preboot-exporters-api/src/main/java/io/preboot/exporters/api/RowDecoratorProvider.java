package io.preboot.exporters.api;

import java.util.List;

/** Provider of row decorators for Excel. Creates decorators based on workbook context and defined columns. */
public interface RowDecoratorProvider {
    /**
     * Provides a row decorator for the specified workbook and columns.
     *
     * @param workbook Workbook for which the decorator is created (in implementation will be
     *     org.apache.poi.ss.usermodel.Workbook)
     * @param columns List of column names
     * @return Created row decorator
     */
    RowDecorator provide(Object workbook, List<String> columns);
}
