package io.preboot.auth.api.event;

import java.util.UUID;

public record UserAccountRemovedFromTenantEvent(UUID userUuid, UUID tenantUuid) {}
