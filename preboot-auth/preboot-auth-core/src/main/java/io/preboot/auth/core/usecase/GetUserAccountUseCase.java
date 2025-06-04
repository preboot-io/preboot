package io.preboot.auth.core.usecase;

import static java.util.stream.Collectors.toSet;

import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.core.model.*;
import io.preboot.auth.core.repository.*;
import io.preboot.query.FilterCriteria;
import io.preboot.query.SearchParams;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserAccountUseCase {
    private final UserAccountRepository userAccountRepository;
    private final UserAccountRolePermissionRepository userAccountRolePermissionRepository;
    private final TenantRepository tenantRepository;
    private final UserAccountTenantRepository userAccountTenantRepository;
    private final UserAccountInfoViewRepository userAccountInfoViewRepository;
    private final TenantRoleRepository tenantRoleRepository;

    @Transactional(readOnly = true)
    public UserAccountInfo execute(final UUID userId, final UUID tenantId) {
        final UserAccount userAccount = userAccountRepository
                .findByUuid(userId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found: " + userId));

        // Check if the user belongs to the tenant
        boolean belongsToTenant = userAccountTenantRepository
                .getByUserAccountUuidAndTenantUuid(userId, tenantId)
                .isPresent();

        final boolean isSuperAdmin = userAccount.isTechnicalAdmin() && tenantId.equals(Tenant.SUPER_ADMIN_TENANT);

        if (!belongsToTenant && !isSuperAdmin) {
            throw new UserAccountNotFoundException("User not found in tenant: " + userId);
        }

        final List<UserAccountRolePermission> rolePermissions =
                userAccountRolePermissionRepository.findAllByRoleIn(userAccount.getTenantRoles(tenantId).stream()
                        .map(UserAccountRole::getName)
                        .collect(toSet()));
        final List<TenantRole> tenantRoles = tenantRoleRepository.findAllByTenantId(tenantId);
        final Set<String> rolePermissionNames =
                rolePermissions.stream().map(UserAccountRolePermission::getName).collect(toSet());
        final Set<String> tenantRoleNames =
                tenantRoles.stream().map(TenantRole::getRoleName).collect(toSet());
        rolePermissionNames.addAll(tenantRoleNames);

        return toDTO(
                userAccount,
                rolePermissionNames,
                isSuperAdmin
                        ? Tenant.createSuperAdminTenant()
                        : tenantRepository.findByUuid(tenantId).orElseThrow());
    }

    private UserAccountInfo toDTO(UserAccount userAccount, final Set<String> rolePermissionNames, final Tenant tenant) {
        return new UserAccountInfo(
                userAccount.getUuid(),
                userAccount.getUsername(),
                userAccount.getEmail(),
                userAccount.getTenantRoles(tenant.getUuid()).stream()
                        .map(UserAccountRole::getName)
                        .collect(toSet()),
                rolePermissionNames,
                userAccount.getTenantPermissions(tenant.getUuid()).stream()
                        .map(UserAccountPermission::getName)
                        .collect(toSet()),
                userAccount.isActive(),
                tenant.getUuid(),
                tenant.getName());
    }

    @Transactional(readOnly = true)
    public Page<UserAccountInfo> getUserAccountsInfo(SearchParams searchParams, final UUID tenantId) {
        searchParams.setFilter(FilterCriteria.eq("tenantId", tenantId));
        return mapUserAccountInfoViews(userAccountInfoViewRepository.findAll(searchParams));
    }

    @Transactional(readOnly = true)
    public Page<UserAccountInfo> getAllUserAccountsInfo(SearchParams searchParams) {
        return mapUserAccountInfoViews(userAccountInfoViewRepository.findAll(searchParams));
    }

    private Page<UserAccountInfo> mapUserAccountInfoViews(Page<UserAccountInfoView> accountInfoViews) {
        return accountInfoViews.map(row -> new UserAccountInfo(
                row.getUuid(),
                row.getUsername(),
                row.getEmail(),
                row.getRoles() != null
                        ? new HashSet<>(Arrays.asList(row.getRoles().split(",")))
                        : new HashSet<>(),
                row.getPermissions() != null
                        ? new HashSet<>(Arrays.asList(row.getPermissions().split(",")))
                        : new HashSet<>(),
                new HashSet<>(), // custom permissions
                row.getActive(),
                row.getTenantId(),
                row.getTenantName()));
    }
}
