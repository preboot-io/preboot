package io.preboot.featureflags.impl;

import java.util.Set;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Data
class TenantFeatureFlag {
    @Id
    private Long id;

    private String name;
    private boolean active;
    private Set<TenantFeatureFlagBinding> tenantBindings;

    @Version
    private Long version;
}
