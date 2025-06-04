package io.preboot.query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@Getter
class JdbcSpecification<T> {
    private final List<FilterCriteria> filterCriteria = new ArrayList<>();
    private CriteriaExpression expression;
    private final CriteriaParameterSource parameterSource;

    public JdbcSpecification() {
        this.parameterSource = new CriteriaParameterSource();
    }

    public JdbcSpecification<T> withCriteria(List<FilterCriteria> criteria) {
        this.filterCriteria.addAll(criteria);

        if (!criteria.isEmpty()) {
            AtomicInteger paramCounter = new AtomicInteger(0);
            this.expression = new CompoundExpression(
                    criteria.stream().map(c -> c.toExpression(paramCounter)).toList(), LogicalOperator.AND);
        }

        return this;
    }

    public List<SearchCriteria> getSearchCriteria() {
        AtomicInteger counter = new AtomicInteger(0);
        return convertToSearchCriteria(filterCriteria, counter);
    }

    private List<SearchCriteria> convertToSearchCriteria(List<FilterCriteria> criteria, AtomicInteger counter) {
        List<SearchCriteria> result = new ArrayList<>();

        for (FilterCriteria fc : criteria) {
            if (fc.isCompound()) {
                List<SearchCriteria> children = fc.getChildren().stream()
                        .map(child -> convertSingleCriteria(child, counter))
                        .collect(Collectors.toList());

                result.add(new SearchCriteria(children, fc.getLogicalOperator(), "group_" + counter.getAndIncrement()));
            } else {
                result.add(convertSingleCriteria(fc, counter));
            }
        }

        return result;
    }

    private SearchCriteria convertSingleCriteria(FilterCriteria fc, AtomicInteger counter) {
        String paramName = fc.getField().replace(".", "_") + "_" + counter.getAndIncrement();
        return new SearchCriteria(
                fc.getField(), convertOperator(fc.getOperator()), fc.getValue(), false, null, null, paramName);
    }

    private String convertOperator(String operator) {
        return SpecificationBuilder.Operator.fromApiOperator(operator).getSqlOperator();
    }

    public boolean hasCriteria() {
        return !filterCriteria.isEmpty();
    }

    public SqlParameterSource getParameterSource() {
        return parameterSource;
    }
}
