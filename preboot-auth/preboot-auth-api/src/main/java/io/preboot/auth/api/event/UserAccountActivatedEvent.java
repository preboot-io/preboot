package io.preboot.auth.api.event;

import java.util.UUID;

public record UserAccountActivatedEvent(UUID userAccountId, String email, String username) {}
