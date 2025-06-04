package io.preboot.auth.api.event;

import java.util.UUID;

public record UserAccountAssignedToTenantEvent(UUID userUuid, UUID tenantUuid) {}
