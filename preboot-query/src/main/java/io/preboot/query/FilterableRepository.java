package io.preboot.query;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FilterableRepository<T, ID> extends CrudRepository<T, ID>, FilterableFragment<T> {}
