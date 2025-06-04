package io.preboot.securedata.context;

import java.util.Set;
import java.util.UUID;

public class SimpleSecurityContext implements SecurityContext {
    private final UUID userId;
    private final UUID tenantId;
    private final Set<String> roles;
    private final Set<String> permissions;

    public SimpleSecurityContext(UUID userId, UUID tenantId, Set<String> roles, Set<String> permissions) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public UUID getUserId() {
        return userId;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public Set<String> getPermissions() {
        return permissions;
    }
}
