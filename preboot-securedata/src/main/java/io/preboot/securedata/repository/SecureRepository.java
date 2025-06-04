package io.preboot.securedata.repository;

import io.preboot.query.FilterableRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SecureRepository<T, ID> extends FilterableRepository<T, ID> {
    // Currently, it just extends FilterableRepository without adding any new methods
    // This means it inherits:
    // - All CrudRepository methods (from FilterableRepository's parent)
    // - All FilterableFragment methods (findAll with SearchParams, etc.)
}
