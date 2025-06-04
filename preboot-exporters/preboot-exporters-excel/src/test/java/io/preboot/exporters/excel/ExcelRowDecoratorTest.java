package io.preboot.exporters.excel;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcelRowDecoratorTest {

    private ExcelRowDecorator decorator;

    @Mock
    private Row mockRow;

    @BeforeEach
    void setUp() {
        decorator = new ExcelRowDecorator();
    }

    @Test
    void decorate_WithNonPoiRow_ShouldNotThrowException() {
        // Arrange
        Object nonPoiRow = new Object();

        // Act & Assert - should not throw exception
        decorator.decorate(nonPoiRow);
    }

    @Test
    void decoratePoiRow_WithExtendedImplementation_ShouldApplyCustomStyling() {
        // Arrange
        XSSFWorkbook workbook = new XSSFWorkbook();
        Row row = workbook.createSheet().createRow(0);
        Cell cell = row.createCell(0);

        ExcelRowDecorator customDecorator = new ExcelRowDecorator() {
            @Override
            protected void decoratePoiRow(Row row) {
                CellStyle style = row.getSheet().getWorkbook().createCellStyle();
                style.setWrapText(true);
                row.getCell(0).setCellStyle(style);
            }
        };

        // Act
        customDecorator.decorate(row);

        // Assert
        assertThat(cell.getCellStyle().getWrapText()).isTrue();
    }
}
