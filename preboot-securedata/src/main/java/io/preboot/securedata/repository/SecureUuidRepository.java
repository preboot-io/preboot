package io.preboot.securedata.repository;

import io.preboot.query.FilterableUuidRepository;
import io.preboot.query.HasUuid;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SecureUuidRepository<T extends HasUuid, ID> extends FilterableUuidRepository<T, ID> {}
