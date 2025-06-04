package io.preboot.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantInfo(UUID uuid, String name, Instant lastUsedAt, boolean active) {}
