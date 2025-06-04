package io.preboot.exporters.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.preboot.exporters.api.ValueTranslator;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExcelServiceTest {

    private ExcelService excelService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueTranslator valueTranslator;

    @Mock
    private HttpServletResponse response;

    private ByteArrayOutputStream outputStream;
    private final Locale locale = Locale.ENGLISH;

    @BeforeEach
    void setUp() {
        excelService = new ExcelService(objectMapper, valueTranslator);
        ReflectionTestUtils.setField(excelService, "serverZoneId", "Europe/Warsaw");
        ReflectionTestUtils.setField(excelService, "windowSize", 100);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void getSupportedFormatAndContentType_ShouldReturnCorrectValues() {
        // Act & Assert
        assertThat(excelService.getSupportedFormat()).isEqualTo("xlsx");
        assertThat(excelService.getContentType().toString())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    void exportToResponse_WithValidData_ShouldGenerateExcelFile() throws IOException {
        // Arrange
        TestData data1 = new TestData(1L, "Test1", new BigDecimal("123.45"), Instant.parse("2023-01-01T10:00:00Z"));
        TestData data2 = new TestData(2L, "Test2", new BigDecimal("456.78"), Instant.parse("2023-01-02T12:00:00Z"));
        List<TestData> dataList = List.of(data1, data2);
        Stream<TestData> dataStream = dataList.stream();

        Map<String, String> labels = Map.of(
                "id", "ID",
                "name", "Name",
                "amount", "Amount",
                "date", "Date");

        // Setup mocks
        setupResponseMock();

        // Use a real ObjectNode instead of a mock
        when(objectMapper.valueToTree(any())).thenReturn(new com.fasterxml.jackson.databind.node.ObjectNode(null));

        // Act
        excelService.exportToResponse("test-file", labels, response, locale, dataStream);

        // Assert
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()));
        assertThat(workbook.getNumberOfSheets()).isEqualTo(1);

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows()).isGreaterThan(0);

        // Verify header row
        Row headerRow = sheet.getRow(0);
        assertThat(headerRow).isNotNull();

        workbook.close();
    }

    @Test
    void exportToResponse_WithFileExtension_ShouldUseCorrectFilename() throws IOException {
        // Arrange
        TestData data = new TestData(1L, "Test", new BigDecimal("123.45"), Instant.now());
        Map<String, String> labels = Map.of("id", "ID");

        // Setup mocks
        setupResponseMock();

        // Use a real ObjectNode instead of a mock
        when(objectMapper.valueToTree(any())).thenReturn(new com.fasterxml.jackson.databind.node.ObjectNode(null));

        ArgumentCaptor<String> contentDispositionCaptor = ArgumentCaptor.forClass(String.class);

        // Act & Assert for file with extension
        excelService.exportToResponse("test-file.xlsx", labels, response, locale, Stream.of(data));
        org.mockito.Mockito.verify(response)
                .setHeader(org.mockito.ArgumentMatchers.eq("Content-Disposition"), contentDispositionCaptor.capture());
        assertThat(contentDispositionCaptor.getValue()).contains("test-file.xlsx");
        assertThat(contentDispositionCaptor.getValue()).doesNotContain("test-file.xlsx.xlsx");

        // Act & Assert for file without extension
        excelService.exportToResponse("test-file", labels, response, locale, Stream.of(data));
        org.mockito.Mockito.verify(response, org.mockito.Mockito.times(2))
                .setHeader(org.mockito.ArgumentMatchers.eq("Content-Disposition"), contentDispositionCaptor.capture());
        assertThat(contentDispositionCaptor.getAllValues().get(1)).contains("test-file.xlsx");
    }

    private void setupResponseMock() throws IOException {
        // Setup response mock
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                outputStream.write(b, off, len);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
                /* Not needed for tests */
            }
        });
    }

    private static class TestData {
        private final Long id;
        private final String name;
        private final BigDecimal amount;
        private final Instant date;

        public TestData(Long id, String name, BigDecimal amount, Instant date) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.date = date;
        }
    }
}
