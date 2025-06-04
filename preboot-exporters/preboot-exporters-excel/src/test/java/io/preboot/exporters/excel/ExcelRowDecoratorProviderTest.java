package io.preboot.exporters.excel;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.exporters.api.RowDecorator;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExcelRowDecoratorProviderTest {

    private ExcelRowDecoratorProvider provider;
    private XSSFWorkbook workbook;

    @BeforeEach
    void setUp() {
        provider = new ExcelRowDecoratorProvider();
        workbook = new XSSFWorkbook();
    }

    @Test
    void provide_WithEmptyColumnList_ShouldReturnExcelRowDecorator() {
        // Act
        RowDecorator decorator = provider.provide(workbook, List.of());

        // Assert
        assertThat(decorator).isNotNull();
        assertThat(decorator).isInstanceOf(ExcelRowDecorator.class);
    }
}
