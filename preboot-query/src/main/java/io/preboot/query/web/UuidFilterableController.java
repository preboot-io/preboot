package io.preboot.query.web;

import io.preboot.exporters.api.DataExporter;
import io.preboot.query.FilterableUuidRepository;
import io.preboot.query.HasUuid;
import io.preboot.query.SearchParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Base controller providing read and filtering functionality for entities with UUID-based operations.
 *
 * @param <T> Entity type that must implement HasUuid
 */
public abstract class UuidFilterableController<T extends HasUuid, ID> {

    private final FilterableUuidRepository<T, ID> repository;
    private final boolean supportsProjections;
    protected final List<DataExporter> dataExporters;

    protected UuidFilterableController(FilterableUuidRepository<T, ID> repository) {
        this(repository, false, Collections.emptyList());
    }

    protected UuidFilterableController(
            FilterableUuidRepository<T, ID> repository, boolean supportsProjections, List<DataExporter> dataExporters) {
        this.repository = repository;
        this.supportsProjections = supportsProjections;
        this.dataExporters = dataExporters;
    }

    // READ
    @Operation(
            summary = "Get entity by UUID",
            parameters = {@Parameter(name = "uuid", in = ParameterIn.PATH, required = true, description = "Entity UUID")
            })
    @GetMapping("/{uuid}")
    public ResponseEntity<T> getByUuid(@PathVariable("uuid") UUID uuid) {
        return repository
                .findByUuid(uuid)
                .map(entity -> {
                    beforeRead(entity);
                    T processedEntity = afterRead(entity);
                    return ResponseEntity.ok(processedEntity);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // SEARCH operations
    @Operation(summary = "Search entities with filtering criteria")
    @PostMapping("/search")
    public Page<T> search(@RequestBody @Valid SearchRequest request) {
        SearchParams params = SearchParams.builder()
                .page(request.page())
                .size(request.size())
                .sortField(request.sortField())
                .sortDirection(request.sortDirection())
                .filters(request.filters())
                .build();

        return repository.findAll(params);
    }

    @Operation(
            summary = "Search entities with projection",
            parameters = {
                @Parameter(
                        name = "projection",
                        in = ParameterIn.PATH,
                        required = true,
                        description = "Projection name",
                        schema = @Schema(type = "string"))
            })
    @PostMapping("/search/{projection}")
    public <P> Page<P> searchProjected(
            @Parameter(description = "Projection name") @PathVariable("projection") String projection,
            @RequestBody @Valid SearchRequest request) {

        if (!supportsProjections) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "Projections are not supported by this controller. Override resolveProjectionClass() to enable projections.");
        }

        Class<P> projectionType = resolveProjectionClass(projection);
        if (projectionType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown projection: " + projection);
        }

        SearchParams params = SearchParams.builder()
                .page(request.page())
                .size(request.size())
                .sortField(request.sortField())
                .sortDirection(request.sortDirection())
                .filters(request.filters())
                .unpaged(request.unpaged())
                .build();

        return repository.findAllProjectedBy(params, projectionType);
    }

    @Operation(summary = "Find one entity matching the criteria")
    @PostMapping("/find")
    public Optional<T> findOne(@RequestBody @Valid SearchRequest request) {
        SearchParams params = SearchParams.builder().filters(request.filters()).build();
        return repository.findOne(params);
    }

    @Operation(summary = "Count entities matching the criteria")
    @PostMapping("/count")
    public long count(@RequestBody @Valid SearchRequest request) {
        SearchParams params = SearchParams.builder().filters(request.filters()).build();
        return repository.count(params);
    }

    /**
     * Exports data to a specified format.
     *
     * @param format Export format (e.g. "xlsx", "pdf", "csv")
     * @param exportRequest Request containing file name and search parameters
     * @param response HTTP response
     * @param locale Locale settings
     * @throws IOException In case of an error during export
     * @throws UnsupportedOperationException If the format is not supported
     */
    @Operation(
            summary = "Export data in specified format",
            description = "Exports filtered data to various formats like CSV, XLSX, PDF",
            parameters = {
                @Parameter(
                        name = "format",
                        in = ParameterIn.PATH,
                        required = true,
                        description = "Export format (e.g. xlsx, pdf, csv)",
                        schema = @Schema(type = "string"))
            })
    @ApiResponse(
            responseCode = "200",
            description = "Successfully exported data",
            content =
                    @Content(
                            mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")))
    @PostMapping("/export/{format}")
    public void exportData(
            @PathVariable String format,
            @RequestBody @Valid ExportRequest exportRequest,
            HttpServletResponse response,
            Locale locale)
            throws IOException {

        DataExporter exporter = findExporterForFormat(format);
        if (exporter == null) {
            throw new UnsupportedOperationException("Export format '" + format + "' is not supported");
        }

        String fileName = exportRequest.fileName() != null ? exportRequest.fileName() : "export";

        SearchParams params = SearchParams.builder()
                .filters(exportRequest.searchRequest().filters())
                .sortField(exportRequest.searchRequest().sortField())
                .sortDirection(exportRequest.searchRequest().sortDirection())
                .build();

        Stream<T> data = repository.findAllAsStream(params);

        Map<String, String> labels = prepareExportLabels();

        response.setContentType(exporter.getContentType().toString());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "." + format + "\"");

        exporter.exportToResponse(fileName, labels, response, locale, data);
    }

    /**
     * Initiates an asynchronous export operation. The export will be processed in the background and the result will be
     * stored as a file.
     *
     * @param format Export format (e.g. "xlsx", "pdf", "csv")
     * @param exportRequest Request containing file name and search parameters
     * @param locale Locale settings
     * @return Response indicating the export has been queued
     * @throws UnsupportedOperationException If the format is not supported
     */
    @Operation(
            summary = "Export data asynchronously in specified format",
            description =
                    "Initiates an async export job that will process the data in the background and store the result as a file",
            parameters = {
                @Parameter(
                        name = "format",
                        in = ParameterIn.PATH,
                        required = true,
                        description = "Export format (e.g. xlsx, pdf, csv)",
                        schema = @Schema(type = "string"))
            })
    @ApiResponse(
            responseCode = "202",
            description = "Export request has been accepted and queued for processing",
            content = @Content(mediaType = "application/json"))
    @PostMapping("/export-async/{format}")
    public ResponseEntity<Map<String, String>> exportDataAsync(
            @PathVariable String format, @RequestBody @Valid ExportRequest exportRequest, Locale locale) {

        // Validate format is supported
        DataExporter exporter = findExporterForFormat(format);
        if (exporter == null) {
            throw new UnsupportedOperationException("Export format '" + format + "' is not supported");
        }

        String fileName = exportRequest.fileName() != null ? exportRequest.fileName() : "export";

        SearchParams params = SearchParams.builder()
                .filters(exportRequest.searchRequest().filters())
                .sortField(exportRequest.searchRequest().sortField())
                .sortDirection(exportRequest.searchRequest().sortDirection())
                .build();

        Map<String, String> labels = prepareExportLabels();

        // Get current user and tenant context
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();
        String repositoryName = getRepositoryName();

        // Create and publish async export event
        AsyncExportEvent exportEvent =
                new AsyncExportEvent(userId, tenantId, format, fileName, params, locale, labels, repositoryName);

        publishAsyncExportEvent(exportEvent);

        // Return accepted response with task information
        Map<String, String> response = Map.of(
                "status",
                "accepted",
                "message",
                "Export request has been queued for processing",
                "format",
                format,
                "fileName",
                fileName + "." + format);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Finds an exporter for the specified format.
     *
     * @param format Export format
     * @return Exporter or null if not found
     */
    protected DataExporter findExporterForFormat(String format) {
        return dataExporters.stream()
                .filter(exporter -> exporter.getSupportedFormat().equalsIgnoreCase(format))
                .findFirst()
                .orElse(null);
    }

    /**
     * Prepares column labels for export. This method can be overridden in derived controllers.
     *
     * @return Map of column labels
     */
    protected Map<String, String> prepareExportLabels() {
        // Default implementation returns an empty map
        // Derived controllers should override this method
        return Map.of();
    }

    // Hook methods for customization
    protected void beforeRead(T entity) {}

    protected T afterRead(T entity) {
        return entity;
    }

    /**
     * Resolves the projection class by name. This method should be implemented by controllers that support projections.
     */
    protected <P> Class<P> resolveProjectionClass(String projectionName) {
        return null;
    }

    /** @return whether this controller supports projections */
    protected boolean supportsProjections() {
        return supportsProjections;
    }

    /** Access to the underlying repository for custom operations. */
    protected FilterableUuidRepository<T, ID> getRepository() {
        return repository;
    }

    // Abstract methods for async export support - to be implemented by concrete controllers
    // This design avoids making preboot-query dependent on preboot-auth or preboot-eventbus

    /**
     * Gets the current user ID from the security context. Implementations should retrieve this from their security
     * context provider.
     *
     * @return Current user ID
     */
    protected abstract UUID getCurrentUserId();

    /**
     * Gets the current tenant ID from the security context. Implementations should retrieve this from their security
     * context provider.
     *
     * @return Current tenant ID
     */
    protected abstract UUID getCurrentTenantId();

    /**
     * Publishes an async export event. Implementations should use their event publisher to publish the event to the
     * appropriate event system.
     *
     * @param event The event to publish
     */
    protected abstract void publishAsyncExportEvent(Object event);

    /**
     * Gets the repository name identifier for async export events. This helps the generic event handler identify which
     * repository to use. Default implementation uses the simple class name of the repository.
     *
     * @return Repository name identifier
     */
    protected String getRepositoryName() {
        return repository.getClass().getSimpleName();
    }
}
