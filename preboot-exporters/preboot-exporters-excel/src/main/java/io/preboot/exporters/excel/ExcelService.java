package io.preboot.exporters.excel;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.preboot.exporters.api.DataExporter;
import io.preboot.exporters.api.RowDecorator;
import io.preboot.exporters.api.RowDecoratorProvider;
import io.preboot.exporters.api.ValueTranslator;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelService implements DataExporter {
    public static final String XLSX_FORMAT = "xlsx";
    public static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(XLSX_CONTENT_TYPE);
    private static final int DEFAULT_WINDOW_SIZE = 100;
    private static final Pattern ISO_INSTANT_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z$");
    private static final Pattern LOCAL_DATE_TIME_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?$");

    @Value("${excel.export.window-size:" + DEFAULT_WINDOW_SIZE + "}")
    private int windowSize;

    @Value("${spring.application.timezone:Europe/Warsaw}")
    private String serverZoneId;

    private final ObjectMapper objectMapper;
    private final ValueTranslator valueTranslator;

    @Override
    public String getSupportedFormat() {
        return XLSX_FORMAT;
    }

    @Override
    public MediaType getContentType() {
        return XLSX_MEDIA_TYPE;
    }

    @Override
    public <T> void exportToResponse(
            String fileName, Map<String, String> labels, HttpServletResponse response, Locale locale, Stream<T> data)
            throws IOException {
        String exportFileName = (fileName != null) ? fileName : "export";

        if (!exportFileName.toLowerCase().endsWith("." + XLSX_FORMAT)) {
            exportFileName = exportFileName + "." + XLSX_FORMAT;
        }

        response.setHeader(CONTENT_DISPOSITION, createUTF8ContentDisposition(exportFileName));
        response.setContentType(XLSX_CONTENT_TYPE);
        try (SXSSFWorkbook workbook = createWorkbook(data, locale, labels)) {
            workbook.write(response.getOutputStream());
        }
    }

    public <T> SXSSFWorkbook createWorkbook(
            Stream<T> data, Locale locale, Map<String, String> labels, RowDecoratorProvider... rowDecoratorProviders) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(this.windowSize);
        workbook.setCompressTempFiles(true);

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle defaultDataStyle = createDefaultDataStyle(workbook);

        processStream(workbook, data, locale, labels, headerStyle, defaultDataStyle, rowDecoratorProviders);
        return workbook;
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDefaultDataStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        return style;
    }

    /**
     * Prepares headers for Excel sheet.
     *
     * @param sheet Sheet
     * @param headerStyle Cell style for headers
     * @param labels Collection of labels (values from the map)
     * @return List of header names
     */
    private List<String> prepareHeaders(Sheet sheet, CellStyle headerStyle, Collection<String> labels) {
        List<String> headerNames = new ArrayList<>(labels.size());
        Row headerRow = sheet.createRow(0);
        int cellIdx = 0;
        for (String label : labels) {
            headerNames.add(label);
            Cell cell = headerRow.createCell(cellIdx++);
            cell.setCellValue(label);
            cell.setCellStyle(headerStyle);
        }
        return headerNames;
    }

    /**
     * Converts a JSON node to a string.
     *
     * @param jsonNode JSON node
     * @return Text representation
     */
    private String asString(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        if (jsonNode.isNumber()) {
            return jsonNode.decimalValue().toPlainString();
        }
        return jsonNode.asText();
    }

    /**
     * Creates a Content-Disposition header with UTF-8 encoding support.
     *
     * @param filename File name
     * @return Content-Disposition header value
     */
    private String createUTF8ContentDisposition(String filename) {
        if (filename != null) {
            filename = filename.replace('"', ' ').replace(',', ' ').trim();
        } else {
            return "attachment";
        }

        StringBuilder contentDisposition = new StringBuilder("attachment");
        CharsetEncoder enc = StandardCharsets.US_ASCII.newEncoder();
        boolean canEncode = enc.canEncode(filename);

        if (canEncode) {
            contentDisposition.append("; filename=\"").append(filename).append("\"");
        } else {
            String encodedFilename;
            try {
                encodedFilename = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8.name())
                        .replace("+", "%20");
            } catch (java.io.UnsupportedEncodingException e) {
                log.warn("UTF-8 encoding not supported for Content-Disposition filename*", e);
                encodedFilename = filename;
            }
            contentDisposition.append("; filename*=UTF-8''").append(encodedFilename);
        }
        return contentDisposition.toString();
    }

    private <T> void processStream(
            SXSSFWorkbook workbook,
            Stream<T> dataStream,
            Locale locale,
            Map<String, String> labels,
            CellStyle headerStyle,
            CellStyle defaultDataStyle,
            RowDecoratorProvider... rowDecoratorProviders) {

        final SXSSFSheet sheet = workbook.createSheet();

        final List<String> headerNames = prepareHeaders(sheet, headerStyle, labels.values());

        List<RowDecorator> decorators = new ArrayList<>();
        if (rowDecoratorProviders != null) {
            for (RowDecoratorProvider provider : rowDecoratorProviders) {
                decorators.add(provider.provide(workbook, new ArrayList<>(labels.keySet())));
            }
        }

        final ZoneId serverZone = ZoneId.of(serverZoneId);
        final DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern("dd.MM.yyyy / HH:mm").withZone(serverZone);

        AtomicInteger rowIdx = new AtomicInteger(0);

        try {
            dataStream.forEach(item -> {
                ObjectNode objectNode = objectMapper.valueToTree(item);
                final Row row = sheet.createRow(rowIdx.incrementAndGet());
                int cellIdx = 0;

                for (String columnKey : labels.keySet()) {
                    final JsonNode rowValue = objectNode.get(columnKey);
                    final int currentCellIdx = cellIdx++;
                    Cell cell = row.createCell(currentCellIdx);

                    cell.setCellStyle(defaultDataStyle);

                    if (rowValue == null || rowValue.isNull()) {
                    } else if (rowValue.isNumber()) {
                        cell.setCellValue(rowValue.asDouble());
                    } else {
                        String stringValue = asString(rowValue);
                        String translatedValue = valueTranslator.translate(columnKey, stringValue, locale);

                        try {
                            if (translatedValue != null) {
                                if (ISO_INSTANT_PATTERN.matcher(translatedValue).matches()) {
                                    Instant instant = Instant.parse(translatedValue);
                                    cell.setCellValue(dateTimeFormatter.format(instant));
                                } else if (LOCAL_DATE_TIME_PATTERN
                                        .matcher(translatedValue)
                                        .matches()) {
                                    LocalDateTime localDateTime = LocalDateTime.parse(translatedValue);
                                    ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(this.serverZoneId));
                                    cell.setCellValue(dateTimeFormatter.format(zonedDateTime));
                                } else {
                                    cell.setCellValue(translatedValue);
                                }
                            } else {
                                cell.setCellValue((String) null);
                            }
                        } catch (DateTimeParseException e) {
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "Excel export: unable to parse date '{}' for column {}. Setting as string.",
                                        translatedValue,
                                        columnKey,
                                        e);
                            }
                            cell.setCellValue(translatedValue);
                        } catch (Exception e) {
                            if (log.isErrorEnabled()) {
                                log.error("Error setting cell value for column {}: {}", columnKey, e.getMessage(), e);
                            }
                            cell.setCellValue(translatedValue);
                        }
                    }
                }

                for (RowDecorator decorator : decorators) {
                    decorator.decorate(row);
                }
            });
        } finally {
            dataStream.close();
        }
    }
}
