package io.preboot.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UseTenantRequest(@NotNull(message = "Tenant UUID is required") UUID tenantId) {}
