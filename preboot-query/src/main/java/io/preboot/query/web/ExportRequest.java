package io.preboot.query.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

/** Request body for export operations. This record combines export file name and search criteria. */
@Schema(
        description = "Export request parameters",
        requiredProperties = {"searchRequest"})
@Builder
public record ExportRequest(
        @Schema(description = "Export file name")
                @Pattern(
                        regexp = "^[a-zA-Z0-9_\\-. ]+$",
                        message =
                                "File name may only contain alphanumeric characters, spaces, dots, hyphens and underscores")
                String fileName,
        @Schema(description = "Search parameters") @NotNull @Valid SearchRequest searchRequest) {

    /** Creates an export request with default file name and empty search request. */
    public static ExportRequest empty() {
        return new ExportRequest("export", SearchRequest.empty());
    }

    /** Creates an export request with specified file name and search request. */
    public static ExportRequest of(String fileName, SearchRequest searchRequest) {
        return new ExportRequest(fileName != null ? fileName : "export", searchRequest);
    }

    /** Creates an export request with default file name and the provided search request. */
    public static ExportRequest withSearchRequest(SearchRequest searchRequest) {
        return new ExportRequest("export", searchRequest);
    }
}
