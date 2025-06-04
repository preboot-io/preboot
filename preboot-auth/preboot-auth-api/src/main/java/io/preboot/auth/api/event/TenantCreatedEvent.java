package io.preboot.auth.api.event;

import java.util.UUID;

public record TenantCreatedEvent(UUID tenantUuid, String name) {}
