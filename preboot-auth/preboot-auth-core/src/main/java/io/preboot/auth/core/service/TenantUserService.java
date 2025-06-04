package io.preboot.auth.core.service;

import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.model.UserAccountRole;
import io.preboot.auth.core.repository.UserAccountRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Service class with utility methods for tenant user administration. */
@Service
@RequiredArgsConstructor
public class TenantUserService {

    private final UserAccountRepository userAccountRepository;

    // List of roles that cannot be assigned by tenant admins
    private static final Set<String> RESTRICTED_ROLES = Set.of("super-admin");

    /**
     * Verifies if a user exists in a specific tenant.
     *
     * @param userId the user ID to check
     * @param tenantId the tenant ID
     * @return the user account if found
     * @throws UserAccountNotFoundException if the user doesn't exist or doesn't belong to the tenant
     */
    public UserAccount verifyUserExistsInTenant(UUID userId, UUID tenantId) {
        UserAccount userAccount = userAccountRepository
                .findByUuid(userId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found: " + userId));

        if (!userAccount.getTenantIds().contains(tenantId)) {
            throw new UserAccountNotFoundException("User " + userId + " does not belong to tenant " + tenantId);
        }

        return userAccount;
    }

    /**
     * Validates a set of roles to ensure they don't include restricted roles.
     *
     * @param roles the roles to validate
     * @throws ResponseStatusException if any role is restricted
     */
    public void validateRoles(Set<String> roles) {
        Set<String> invalidRoles =
                roles.stream().filter(RESTRICTED_ROLES::contains).collect(Collectors.toSet());

        if (!invalidRoles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cannot assign restricted roles: " + String.join(", ", invalidRoles));
        }
    }

    /**
     * Checks if the user has any role in the tenant.
     *
     * @param userId the user ID to check
     * @param tenantId the tenant ID
     * @return true if the user has at least one role in the tenant
     */
    public boolean hasRolesInTenant(UUID userId, UUID tenantId) {
        UserAccount userAccount = userAccountRepository
                .findByUuid(userId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found: " + userId));

        List<UserAccountRole> tenantRoles = userAccount.getRoles().stream()
                .filter(role -> role.getTenantId().equals(tenantId))
                .toList();

        return !tenantRoles.isEmpty();
    }
}
