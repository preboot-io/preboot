package io.preboot.auth.api.dto;

import java.util.Set;
import java.util.UUID;

public record TenantResponse(UUID uuid, String name, Set<String> roles, boolean active, boolean demo) {}
