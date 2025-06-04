package io.preboot.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TenantUpdateRequest(
        @NotNull(message = "Tenant UUID is required") UUID tenantId,
        @NotBlank(message = "Organization name is required") String name,
        @NotNull(message = "Active status is required") Boolean active) {}
