package io.preboot.featureflags.impl;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface FeatureFlagRepository extends CrudRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByName(String name);
}
