package io.preboot.exporters.excel;

import io.preboot.exporters.api.RowDecorator;
import org.apache.poi.ss.usermodel.Row;

/** Implementation of RowDecorator for Apache POI Excel sheets. */
public class ExcelRowDecorator implements RowDecorator {
    @Override
    public void decorate(Object row) {
        if (row instanceof Row poiRow) {
            decoratePoiRow(poiRow);
        }
    }

    /**
     * Actual method for decorating POI row.
     *
     * @param row POI row to decorate
     */
    protected void decoratePoiRow(Row row) {
        // Default implementation doesn't perform any operations
        // Derived classes can override this method
    }
}
