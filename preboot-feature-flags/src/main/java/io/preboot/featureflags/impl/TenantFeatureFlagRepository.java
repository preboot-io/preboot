package io.preboot.featureflags.impl;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface TenantFeatureFlagRepository extends CrudRepository<TenantFeatureFlag, Long> {
    Optional<TenantFeatureFlag> findByName(String name);
}
