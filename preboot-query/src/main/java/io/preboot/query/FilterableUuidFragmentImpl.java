package io.preboot.query;

import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class FilterableUuidFragmentImpl<T extends HasUuid, ID> extends FilterableFragmentImpl<T, ID>
        implements UuidRepository<T> {

    protected FilterableUuidFragmentImpl(FilterableFragmentContext context, Class<T> entityClass) {
        super(context, entityClass);
    }

    @Override
    public Optional<T> findByUuid(UUID uuid) {
        return findOne(SearchParams.criteria(FilterCriteria.eq("uuid", uuid)).build());
    }

    @Override
    public boolean existsByUuid(UUID uuid) {
        return count(SearchParams.criteria(FilterCriteria.eq("uuid", uuid)).build()) > 0;
    }

    @Override
    @Transactional
    public void deleteByUuid(UUID uuid) {
        findByUuid(uuid).ifPresent(this::delete);
    }
}
