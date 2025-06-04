package io.preboot.securedata.context;

import java.util.Set;
import java.util.UUID;

/** Context object containing security information needed for ownership checks. */
public interface SecurityContext {
    /** Gets the ID of the current user. */
    UUID getUserId();

    /** Gets the ID of the tenant the current user belongs to. */
    UUID getTenantId();

    /** Gets the list of roles assigned to the current user. */
    Set<String> getRoles();

    /** Gets the list of all permissions assigned to the current user. */
    Set<String> getPermissions();
}
