package io.preboot.featureflags.impl;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class TenantFeatureFlagBinding {
    private UUID tenantId;
}
