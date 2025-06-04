package io.preboot.auth.core.repository;

import io.preboot.auth.core.model.Tenant;
import io.preboot.query.FilterableRepository;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends FilterableRepository<Tenant, Long> {
    Optional<Tenant> findByUuid(UUID uuid);

    boolean existsByName(String name);
}
