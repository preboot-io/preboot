package io.preboot.auth.api.event;

import java.util.UUID;

public record UserAccountCreatedEvent(UUID tenantId, UUID userAccountId, String email, String username) {}
