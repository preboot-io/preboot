package io.preboot.auth.api.event;

import java.util.UUID;

public record TenantDeletedEvent(UUID tenantUuid) {}
