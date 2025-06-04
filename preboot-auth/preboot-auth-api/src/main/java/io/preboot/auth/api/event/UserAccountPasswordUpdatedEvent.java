package io.preboot.auth.api.event;

import java.util.UUID;

public record UserAccountPasswordUpdatedEvent(UUID userAccountId, String email, String username) {}
