package io.preboot.query.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.preboot.query.SearchParams;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Event published when an asynchronous export operation is requested. Contains all necessary information to process the
 * export in a background task.
 *
 * @param userId ID of the user requesting the export
 * @param tenantId ID of the tenant context
 * @param format Export format (e.g., "xlsx", "csv", "pdf")
 * @param requestedFileName Base filename for the export (without extension)
 * @param searchParams Search parameters to filter the exported data
 * @param locale Locale for formatting and localization
 * @param labels Column labels for the export
 * @param repositoryName Name identifier for the repository to export from
 */
public record AsyncExportEvent(
        @NotNull UUID userId,
        @NotNull UUID tenantId,
        @NotBlank String format,
        @NotBlank String requestedFileName,
        @Valid @NotNull SearchParams searchParams,
        @NotNull Locale locale,
        @NotNull Map<String, String> labels,
        @NotBlank String repositoryName) {

    @JsonCreator
    public AsyncExportEvent(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("tenantId") UUID tenantId,
            @JsonProperty("format") String format,
            @JsonProperty("requestedFileName") String requestedFileName,
            @JsonProperty("searchParams") SearchParams searchParams,
            @JsonProperty("locale") Locale locale,
            @JsonProperty("labels") Map<String, String> labels,
            @JsonProperty("repositoryName") String repositoryName) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.format = format;
        this.requestedFileName = requestedFileName;
        this.searchParams = searchParams;
        this.locale = locale;
        this.labels = labels;
        this.repositoryName = repositoryName;
    }
}
