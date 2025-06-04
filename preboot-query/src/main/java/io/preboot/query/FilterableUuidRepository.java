package io.preboot.query;

import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FilterableUuidRepository<T extends HasUuid, ID>
        extends FilterableRepository<T, ID>, UuidRepository<T> {}
