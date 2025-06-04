package io.preboot.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record TenantUserAssignRequest(@NotNull UUID userUuid, @NotNull Set<String> roles // organizational roles
        ) {}
