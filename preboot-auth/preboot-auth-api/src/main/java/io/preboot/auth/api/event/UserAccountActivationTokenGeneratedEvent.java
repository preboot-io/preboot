package io.preboot.auth.api.event;

import java.util.UUID;

public record UserAccountActivationTokenGeneratedEvent(
        UUID userAccountId, String email, String username, String token) {}
