package io.preboot.auth.api.event;

import java.util.UUID;

public record UserAccountPasswordResetTokenGeneratedEvent(
        UUID userAccountId, String username, String email, String token) {}
