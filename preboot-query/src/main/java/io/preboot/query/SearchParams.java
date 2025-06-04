package io.preboot.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Sort;

@Data
@Builder
@Accessors(chain = true)
public class SearchParams {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.ASC;
    public static final String DEFAULT_SORT_FIELD = "id";

    private List<FilterCriteria> filters;

    @Builder.Default
    private Integer page = DEFAULT_PAGE;

    @Builder.Default
    private Integer size = DEFAULT_SIZE;

    @Builder.Default
    private String sortField = DEFAULT_SORT_FIELD;

    @Builder.Default
    private Sort.Direction sortDirection = DEFAULT_DIRECTION;

    @Builder.Default
    private boolean unpaged = false;

    public static SearchParams empty() {
        return SearchParams.builder().filters(new ArrayList<>()).build();
    }

    public static SearchParamsBuilder criteria(FilterCriteria... criteria) {
        return SearchParams.builder().filters(Arrays.asList(criteria));
    }

    public List<FilterCriteria> getFilters() {
        if (filters == null) {
            filters = new ArrayList<>();
        }
        return filters;
    }

    public SearchParams setFilter(FilterCriteria filter) {
        getFilters().removeIf(f -> f.getField().equals(filter.getField()));
        getFilters().add(filter);
        return this;
    }
}
