package io.preboot.exporters.excel;

import io.preboot.exporters.api.RowDecorator;
import io.preboot.exporters.api.RowDecoratorProvider;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;

/** Base implementation of RowDecoratorProvider for Excel. */
public class ExcelRowDecoratorProvider implements RowDecoratorProvider {
    @Override
    public RowDecorator provide(Object workbook, List<String> columns) {
        if (workbook instanceof Workbook) {
            return new ExcelRowDecorator();
        }
        throw new IllegalArgumentException("Workbook must be instance of org.apache.poi.ss.usermodel.Workbook");
    }
}
