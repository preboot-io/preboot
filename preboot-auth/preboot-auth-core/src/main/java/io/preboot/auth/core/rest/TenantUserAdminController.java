package io.preboot.auth.core.rest;

import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateInactiveUserAccountRequest;
import io.preboot.auth.api.dto.TenantUserAssignRequest;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.api.guard.TenantAdminRoleAccessGuard;
import io.preboot.auth.api.resolver.TenantResolver;
import io.preboot.auth.core.service.TenantUserService;
import io.preboot.auth.core.spring.SessionAwareAuthentication;
import io.preboot.query.SearchParams;
import io.preboot.query.web.SearchRequest;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** REST controller for tenant administrators to manage users within their tenant. */
@RestController
@RequestMapping("/api/tenant/users")
@RequiredArgsConstructor
public class TenantUserAdminController {
    private final UserAccountManagementApi userAccountManagementApi;
    private final TenantResolver tenantResolver;
    private final TenantUserService tenantUserService;

    /** Search users with criteria. For a simple list of all users, send an empty filter list. */
    @PostMapping("/search")
    @TenantAdminRoleAccessGuard
    public Page<UserAccountInfo> searchUsers(@RequestBody @Valid SearchRequest request) {
        UUID tenantId = tenantResolver.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException(
                    "Current tenant ID is null. Make sure you are authenticated and have selected a tenant.");
        }
        SearchParams params = SearchParams.builder()
                .page(request.page())
                .size(request.size())
                .sortField(request.sortField())
                .sortDirection(request.sortDirection())
                .filters(request.filters())
                .unpaged(request.unpaged())
                .build();
        return userAccountManagementApi.getUserAccountsInfo(params, tenantId);
    }

    /** Gets a specific user by their ID within the current tenant. */
    @GetMapping("/{userId}")
    @TenantAdminRoleAccessGuard
    public UserAccountInfo getUser(@PathVariable UUID userId) {
        UUID tenantId = tenantResolver.getCurrentTenant();
        return userAccountManagementApi.getUserAccount(userId, tenantId);
    }

    /** Creates a new inactive user account within the current tenant. */
    @PostMapping
    @TenantAdminRoleAccessGuard
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountInfo createUser(@RequestBody @Valid CreateInactiveUserAccountRequest request) {
        UUID tenantId = tenantResolver.getCurrentTenant();
        // Validate roles before creating the user
        tenantUserService.validateRoles(request.roles());
        // Ensure the tenantId is set to the current tenant
        CreateInactiveUserAccountRequest tenantRequest = new CreateInactiveUserAccountRequest(
                request.username(),
                request.email(),
                request.language(),
                request.timezone(),
                request.roles(),
                request.permissions(),
                tenantId);
        return userAccountManagementApi.createUserAccountForTenant(tenantRequest);
    }

    /** Updates a user's roles within the current tenant. */
    @PutMapping("/{userId}/roles")
    @TenantAdminRoleAccessGuard
    public UserAccountInfo updateUserRoles(
            @PathVariable UUID userId, @RequestBody @Valid TenantUserAssignRequest request) {
        UUID tenantId = tenantResolver.getCurrentTenant();
        // Verify the user exists in the tenant
        tenantUserService.verifyUserExistsInTenant(userId, tenantId);

        // Check if this is a self-modification request
        UUID currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            // Check if ADMIN role is being removed
            if (!request.roles().contains("ADMIN")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove your own ADMIN role");
            }
        }

        // Validate the roles being assigned
        tenantUserService.validateRoles(request.roles());

        // Remove existing roles
        UserAccountInfo userInfo = userAccountManagementApi.getUserAccount(userId, tenantId);
        userInfo.roles().forEach(role -> userAccountManagementApi.removeRole(userId, tenantId, role));

        // Add new roles
        UserAccountInfo updatedUser = userInfo;
        for (String role : request.roles()) {
            updatedUser = userAccountManagementApi.addRole(userId, tenantId, role);
        }
        return updatedUser;
    }

    /** Removes a user from the current tenant. */
    @DeleteMapping("/{userId}")
    @TenantAdminRoleAccessGuard
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUser(@PathVariable UUID userId) {
        UUID tenantId = tenantResolver.getCurrentTenant();

        // Prevent self-removal
        UUID currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove yourself from the tenant");
        }

        userAccountManagementApi.removeUser(userId, tenantId);
    }

    /** Adds a role to a user. */
    @PostMapping("/{userId}/roles/{roleName}")
    @TenantAdminRoleAccessGuard
    public UserAccountInfo addRole(@PathVariable UUID userId, @PathVariable String roleName) {
        UUID tenantId = tenantResolver.getCurrentTenant();
        // Verify the user exists in the tenant
        tenantUserService.verifyUserExistsInTenant(userId, tenantId);
        // Validate the role
        tenantUserService.validateRoles(Set.of(roleName));
        return userAccountManagementApi.addRole(userId, tenantId, roleName);
    }

    /** Removes a role from a user. */
    @DeleteMapping("/{userId}/roles/{roleName}")
    @TenantAdminRoleAccessGuard
    public UserAccountInfo removeRole(@PathVariable UUID userId, @PathVariable String roleName) {
        UUID tenantId = tenantResolver.getCurrentTenant();

        // Prevent self-removal of ADMIN role
        UUID currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId) && "ADMIN".equals(roleName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove your own ADMIN role");
        }

        // Verify the user exists in the tenant
        tenantUserService.verifyUserExistsInTenant(userId, tenantId);
        return userAccountManagementApi.removeRole(userId, tenantId, roleName);
    }

    /** Helper method to get the current authenticated user's UUID */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof SessionAwareAuthentication) {
            UserAccountInfo userInfo = (UserAccountInfo) auth.getPrincipal();
            return userInfo.uuid();
        }
        throw new IllegalStateException("Unable to determine current user");
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<String> handleUserAccountNotFoundException(UserAccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
