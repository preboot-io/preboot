package io.preboot.auth.api.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record UserAccountInfo(
        UUID uuid,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions,
        Set<String> customPermissions,
        boolean active,
        UUID tenantId,
        String tenantName) {

    public Set<String> getAllPermissions() {
        Set<String> allPermissions = new HashSet<>(permissions);
        allPermissions.addAll(customPermissions);
        return allPermissions;
    }
}
