package io.preboot.auth.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;

@Validated
public record ResentActivationLinkCommand(@NotNull UUID userId, @Nullable UUID tenantId) {}
