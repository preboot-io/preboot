package io.preboot.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

public record CreateInactiveUserAccountRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        String language,
        String timezone,
        Set<String> roles,
        Set<String> permissions,
        UUID tenantId) {}
