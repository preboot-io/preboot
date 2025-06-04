package io.preboot.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantRequest(
        @NotBlank(message = "Tenant name is required") String name,
        @NotNull(message = "Active status is required") Boolean active) {}
