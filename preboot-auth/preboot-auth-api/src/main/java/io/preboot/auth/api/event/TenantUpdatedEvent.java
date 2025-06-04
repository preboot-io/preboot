package io.preboot.auth.api.event;

import java.util.UUID;

public record TenantUpdatedEvent(UUID tenantUuid, String name) {}
