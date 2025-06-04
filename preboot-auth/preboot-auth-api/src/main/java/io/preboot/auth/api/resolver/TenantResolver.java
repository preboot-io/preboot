package io.preboot.auth.api.resolver;

import java.util.UUID;

public interface TenantResolver {
    UUID getCurrentTenant();
}
