package io.preboot.query;

import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;

interface FilterableFragment<T> {
    Page<T> findAll(SearchParams params);

    Stream<T> findAllAsStream(SearchParams params);

    Optional<T> findOne(SearchParams params);

    long count(SearchParams params);

    <P> Page<P> findAllProjectedBy(SearchParams params, Class<P> projectionType);

    <P> Stream<P> findAllProjectedByAsStream(SearchParams params, Class<P> projectionType);

    <P> Optional<P> findOneProjectedBy(SearchParams params, Class<P> projectionType);
}
