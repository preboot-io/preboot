package io.preboot.exporters.api;

/**
 * Interface for Excel row decorators. Allows customization of the appearance and behavior of a row.
 *
 * <p>The concrete implementation will use org.apache.poi classes, but the interface can remain independent.
 */
public interface RowDecorator {
    /**
     * Decorates a row in an Excel sheet.
     *
     * @param row Row to decorate (in implementation will be org.apache.poi.ss.usermodel.Row)
     */
    void decorate(Object row);
}
