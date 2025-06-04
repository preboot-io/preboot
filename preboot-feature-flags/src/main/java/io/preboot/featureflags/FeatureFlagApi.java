package io.preboot.featureflags;

import java.util.UUID;

public interface FeatureFlagApi {
    void setFlag(UUID tenantId, String flagName, boolean enabled);

    boolean isEnabled(UUID tenantId, String flagName);

    void setGlobalFlag(String flagName, boolean enabled);

    boolean isGlobalFlagEnabled(String flagName);
}
