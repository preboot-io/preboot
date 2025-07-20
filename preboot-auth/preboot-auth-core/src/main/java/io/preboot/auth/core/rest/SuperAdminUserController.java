package io.preboot.auth.core.rest;

import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateInactiveUserAccountRequest;
import io.preboot.auth.api.dto.ResentActivationLinkCommand;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.api.guard.SuperAdminRoleAccessGuard;
import io.preboot.query.SearchParams;
import io.preboot.query.web.SearchRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for super-admin to manage users across all tenants. */
@RestController
@RequestMapping("/api/super-admin/users")
@RequiredArgsConstructor
@SuperAdminRoleAccessGuard
public class SuperAdminUserController {

    private final UserAccountManagementApi userAccountManagementApi;

    /** Search users across all tenants. */
    @PostMapping("/search")
    public Page<UserAccountInfo> searchAllUsers(@RequestBody @Valid SearchRequest request) {
        SearchParams params = SearchParams.builder()
                .page(request.page())
                .size(request.size())
                .sortField(request.sortField())
                .sortDirection(request.sortDirection())
                .filters(request.filters())
                .unpaged(request.unpaged())
                .build();

        return userAccountManagementApi.getAllUserAccountsInfo(params);
    }

    /** Create a new user in a specific tenant. */
    @PostMapping("/tenant/{tenantId}")
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountInfo createUserForTenant(
            @PathVariable UUID tenantId, @RequestBody @Valid CreateInactiveUserAccountRequest request) {

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

    /** Get a specific user by ID. */
    @GetMapping("/{userId}/tenant/{tenantId}")
    public UserAccountInfo getUser(@PathVariable UUID userId, @PathVariable UUID tenantId) {
        return userAccountManagementApi.getUserAccount(userId, tenantId);
    }

    /** Get all users for a specific tenant. */
    @PostMapping("/tenant/{tenantId}/search")
    public Page<UserAccountInfo> getTenantUsers(
            @PathVariable UUID tenantId, @RequestBody @Valid SearchRequest request) {
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

    /** Remove a user from a specific tenant. */
    @DeleteMapping("/{userId}/tenant/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUserFromTenant(@PathVariable UUID userId, @PathVariable UUID tenantId) {
        userAccountManagementApi.removeUser(userId, tenantId);
    }

    /** Add a role to a user in a specific tenant. */
    @PostMapping("/{userId}/tenant/{tenantId}/roles/{roleName}")
    public UserAccountInfo addRole(
            @PathVariable UUID userId, @PathVariable UUID tenantId, @PathVariable String roleName) {

        return userAccountManagementApi.addRole(userId, tenantId, roleName);
    }

    /** Remove a role from a user in a specific tenant. */
    @DeleteMapping("/{userId}/tenant/{tenantId}/roles/{roleName}")
    public UserAccountInfo removeRole(
            @PathVariable UUID userId, @PathVariable UUID tenantId, @PathVariable String roleName) {

        return userAccountManagementApi.removeRole(userId, tenantId, roleName);
    }

    /** Send activation link again to the inactive user */
    @PostMapping("/{userId}/resend-activation-link")
    public void resendActivationLink(@PathVariable UUID userId) {
        userAccountManagementApi.resendActivationLink(new ResentActivationLinkCommand(userId, null));
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<String> handleUserAccountNotFoundException(UserAccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
