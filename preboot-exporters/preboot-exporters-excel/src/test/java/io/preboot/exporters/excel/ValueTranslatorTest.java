package io.preboot.exporters.excel;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.exporters.api.ValueTranslator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ValueTranslatorTest {

    @Test
    void defaultValueTranslator_ShouldReturnOriginalValue() {
        // Arrange
        DefaultValueTranslator translator = new DefaultValueTranslator();
        String testValue = "test_value";

        // Act
        String result = translator.translate("anyColumn", testValue, Locale.getDefault());

        // Assert
        assertThat(result).isEqualTo(testValue);
    }

    @Test
    void customValueTranslator_ShouldTranslateValues() {
        // Arrange
        TestValueTranslator translator = new TestValueTranslator();
        Locale plLocale = new Locale("pl", "PL");
        Locale enLocale = Locale.ENGLISH;

        // Act & Assert - Polish locale
        assertThat(translator.translate("status", "NEW", plLocale)).isEqualTo("Nowy");
        assertThat(translator.translate("status", "IN_PROGRESS", plLocale)).isEqualTo("W trakcie");
        assertThat(translator.translate("status", "COMPLETED", plLocale)).isEqualTo("Zakończony");

        // Act & Assert - English locale
        assertThat(translator.translate("status", "COMPLETED", enLocale)).isEqualTo("Completed");
        assertThat(translator.translate("status", "NEW", enLocale)).isEqualTo("New");

        // Non-translated values
        assertThat(translator.translate("name", "Test", plLocale)).isEqualTo("Test");
        assertThat(translator.translate("status", "UNKNOWN", plLocale)).isEqualTo("UNKNOWN");
    }

    @Test
    void translatorIntegrationWithExcelService() throws IOException {
        // Integration test for translator with ExcelService
        // This is a simplified version that demonstrates the key concepts

        // Setup
        TestValueTranslator translator = new TestValueTranslator();
        ExcelServiceTestHelper helper = new ExcelServiceTestHelper(translator);

        TestEntity entity = new TestEntity(1L, "COMPLETED", new BigDecimal("100.50"));
        Map<String, String> labels = Map.of("id", "ID", "status", "Status", "amount", "Amount");

        // Test with Polish locale
        Workbook plWorkbook = helper.generateExcel(Stream.of(entity), labels, new Locale("pl", "PL"));
        Sheet plSheet = plWorkbook.getSheetAt(0);
        Row plDataRow = plSheet.getRow(1);

        // Find column indexes
        Row headerRow = plSheet.getRow(0);
        int statusColumnIndex = findColumnIndex(headerRow, "Status");

        // Verify translation
        assertThat(plDataRow.getCell(statusColumnIndex).getStringCellValue()).isEqualTo("Zakończony");

        // Test with English locale
        Workbook enWorkbook = helper.generateExcel(Stream.of(entity), labels, Locale.ENGLISH);
        Sheet enSheet = enWorkbook.getSheetAt(0);
        Row enDataRow = enSheet.getRow(1);

        // Verify translation
        assertThat(enDataRow.getCell(statusColumnIndex).getStringCellValue()).isEqualTo("Completed");
    }

    private int findColumnIndex(Row headerRow, String columnName) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && cell.getCellType() == CellType.STRING) {
                if (columnName.equals(cell.getStringCellValue())) {
                    return i;
                }
            }
        }
        return -1;
    }

    // Helper class for integration testing
    private static class ExcelServiceTestHelper {
        private final ValueTranslator translator;

        public ExcelServiceTestHelper(ValueTranslator translator) {
            this.translator = translator;
        }

        public <T> Workbook generateExcel(Stream<T> data, Map<String, String> labels, Locale locale)
                throws IOException {
            ExcelService service = new ExcelService(new com.fasterxml.jackson.databind.ObjectMapper(), translator);
            ReflectionTestUtils.setField(service, "serverZoneId", "Europe/Warsaw");
            ReflectionTestUtils.setField(service, "windowSize", 100);

            return service.createWorkbook(data, locale, labels);
        }
    }

    // Test entity
    private static class TestEntity {
        private final Long id;
        private final String status;
        private final BigDecimal amount;

        public TestEntity(Long id, String status, BigDecimal amount) {
            this.id = id;
            this.status = status;
            this.amount = amount;
        }

        public Long getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    // Custom value translator for testing
    private static class TestValueTranslator implements ValueTranslator {
        @Override
        public String translate(String column, String value, Locale locale) {
            if (column.equals("status") && value != null) {
                if (locale.getLanguage().equals("pl")) {
                    return switch (value) {
                        case "NEW" -> "Nowy";
                        case "IN_PROGRESS" -> "W trakcie";
                        case "COMPLETED" -> "Zakończony";
                        case "CANCELLED" -> "Anulowany";
                        default -> value;
                    };
                } else {
                    return switch (value) {
                        case "NEW" -> "New";
                        case "IN_PROGRESS" -> "In Progress";
                        case "COMPLETED" -> "Completed";
                        case "CANCELLED" -> "Cancelled";
                        default -> value;
                    };
                }
            }
            return value;
        }
    }
}
