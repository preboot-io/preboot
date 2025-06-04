package io.preboot.query.web;

import io.preboot.query.FilterCriteria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Builder;
import org.springframework.data.domain.Sort;

/** Request body for search operations. This record defines the structure of the JSON payload for search requests. */
@Schema(description = "Search request parameters")
@Builder
public record SearchRequest(
        @Schema(description = "Page number (0-based)") @Min(0) Integer page,
        @Schema(description = "Page size") @Min(1) @Max(100) Integer size,
        @Schema(description = "Sort field") @Pattern(regexp = "^[a-zA-Z0-9_]+$") String sortField,
        @Schema(description = "Sort direction") Sort.Direction sortDirection,
        @Schema(description = "Filter criteria") List<FilterCriteria> filters,
        @Schema(description = "Whether to return all results without paging") boolean unpaged) {
    /** Creates an empty search request with default pagination. */
    public static SearchRequest empty() {
        return new SearchRequest(0, 20, null, Sort.Direction.ASC, List.of(), false);
    }

    /** Creates a search request with the specified page and size. */
    public static SearchRequest of(int page, int size) {
        return new SearchRequest(page, size, null, Sort.Direction.ASC, List.of(), false);
    }

    /** Creates a search request with the specified filters. */
    public static SearchRequest withFilters(List<FilterCriteria> filters) {
        return new SearchRequest(0, 20, null, Sort.Direction.ASC, filters, false);
    }

    /** Creates a search request with sorting configuration. */
    public static SearchRequest withSort(String sortField, Sort.Direction direction) {
        return new SearchRequest(0, 20, sortField, direction, List.of(), false);
    }

    public static SearchRequest all() {
        return new SearchRequest(null, null, null, null, List.of(), true);
    }
}
