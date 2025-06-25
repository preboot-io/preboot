package io.preboot.exporters.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.http.MediaType;

/**
 * Interface for data exporters to various formats. The implementation should handle a specific export format (e.g.
 * Excel, PDF, CSV).
 */
public interface DataExporter {

    /**
     * Returns the format supported by this exporter.
     *
     * @return Format identifier (e.g. "xlsx", "pdf", "csv")
     */
    String getSupportedFormat();

    /**
     * Returns the MIME content type for the format supported by the exporter.
     *
     * @return MIME type (e.g. "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
     */
    MediaType getContentType();

    /**
     * Exports data to a specified format and writes it to the HTTP response.
     *
     * @param fileName The name of the file to be generated
     * @param labels Map of column labels. Key is the field name in the object, value is the header label
     * @param response HTTP response to which the file will be written
     * @param locale Locale settings
     * @param data Data to export
     * @param <T> Entity type
     * @throws IOException In case of writing error
     */
    <T> void exportToResponse(
            String fileName, Map<String, String> labels, HttpServletResponse response, Locale locale, Stream<T> data)
            throws IOException;

    /**
     * Exports data to a specified format and returns an OutputStream.
     *
     * @param fileName The name of the file to be generated
     * @param labels Map of column labels. Key is the field name in the object, value is the header label
     * @param outputStream The output stream to write to
     * @param locale Locale settings
     * @param data Data to export
     * @param <T> Entity type
     * @throws IOException In case of writing error
     */
    <T> void exportToOutputStream(
            String fileName, Map<String, String> labels, OutputStream outputStream, Locale locale, Stream<T> data)
            throws IOException;
}
