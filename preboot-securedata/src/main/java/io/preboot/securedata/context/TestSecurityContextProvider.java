package io.preboot.securedata.context;

import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public class TestSecurityContextProvider implements SecurityContextProvider {
    private UUID userId;
    private UUID tenantId;
    private Set<String> roles;
    private Set<String> permissions;

    public TestSecurityContextProvider(UUID userId, UUID tenantId, Set<String> roles, Set<String> permissions) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roles = roles;
        this.permissions = permissions;
    }

    public TestSecurityContextProvider(final Set<String> roles, final Set<String> permissions) {
        this.userId = UUID.randomUUID();
        this.tenantId = UUID.randomUUID();
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public SecurityContext getCurrentContext() {
        return new SimpleSecurityContext(userId, tenantId, roles, permissions);
    }
}
