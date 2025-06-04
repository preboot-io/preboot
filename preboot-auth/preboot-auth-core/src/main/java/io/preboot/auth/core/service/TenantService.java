package io.preboot.auth.core.service;

import io.preboot.auth.api.dto.CreateTenantRequest;
import io.preboot.auth.api.dto.TenantResponse;
import io.preboot.auth.api.dto.TenantUpdateRequest;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.event.TenantCreatedEvent;
import io.preboot.auth.api.event.TenantDeletedEvent;
import io.preboot.auth.api.event.TenantUpdatedEvent;
import io.preboot.auth.core.model.Tenant;
import io.preboot.auth.core.model.TenantRole;
import io.preboot.auth.core.model.UserAccountTenant;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.TenantRoleRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.core.spring.AuthAccountProperties; // Added import
import io.preboot.auth.core.usecase.GetUserAccountUseCase;
import io.preboot.auth.core.usecase.RemoveUserAccountUseCase;
import io.preboot.core.validation.BeanValidator;
import io.preboot.eventbus.EventPublisher;
import io.preboot.query.SearchParams;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;
    private final TenantRoleRepository tenantRoleRepository;
    private final EventPublisher eventPublisher;
    private final GetUserAccountUseCase getUserAccountUseCase;
    private final UserAccountTenantRepository userAccountTenantRepository;
    private final RemoveUserAccountUseCase removeUserAccountUseCase;
    private final AuthAccountProperties authAccountProperties;

    public static final String DEMO_TENANT_ROLE = "DEMO";

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        BeanValidator.validate(request);

        // Check if tenant with the same name already exists to provide a better error message
        if (tenantRepository.existsByName(request.name())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Tenant with name '" + request.name() + "' already exists.");
        }

        Tenant tenant = new Tenant()
                .setUuid(UUID.randomUUID())
                .setName(request.name())
                .setCreatedAt(Instant.now())
                .setActive(request.active());

        tenant = tenantRepository.save(tenant);
        eventPublisher.publish(new TenantCreatedEvent(tenant.getUuid(), tenant.getName()));

        // Automatically assign DEMO role to new tenant if configured
        if (authAccountProperties.isAssignDemoRoleToNewTenants()) {
            // Using a constant for the role name
            addRoleToTenant(tenant.getUuid(), DEMO_TENANT_ROLE);
        }

        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(UUID uuid) {
        return tenantRepository
                .findByUuid(uuid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> getTenants(SearchParams params) {
        final Page<Tenant> tenants = tenantRepository.findAll(params);

        return tenants.map(this::mapToResponse);
    }

    @Transactional
    public TenantResponse updateTenant(UUID uuid, TenantUpdateRequest request) {
        Tenant tenant = tenantRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        // Check if updating to a name that already exists (and is not the current tenant's name)
        if (!tenant.getName().equals(request.name()) && tenantRepository.existsByName(request.name())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Tenant with name '" + request.name() + "' already exists.");
        }

        tenant.setName(request.name());
        tenant.setActive(request.active());
        tenant = tenantRepository.save(tenant);

        eventPublisher.publish(new TenantUpdatedEvent(tenant.getUuid(), tenant.getName()));
        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public boolean isDemoTenant(UUID uuid) {
        return getTenant(uuid).roles().contains(DEMO_TENANT_ROLE);
    }

    @Transactional(readOnly = true)
    public List<UserAccountInfo> getTenantUserAccounts(UUID tenantId) {
        tenantRepository
                .findByUuid(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        final List<UserAccountTenant> accounts = userAccountTenantRepository.findAllByTenantUuid(tenantId);

        return accounts.stream()
                .map(userOrg -> getUserAccountUseCase.execute(userOrg.getUserAccountUuid(), tenantId))
                .toList();
    }

    @Transactional
    public void deleteTenant(UUID uuid) {
        Tenant tenant = tenantRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        // Before deleting a tenant, ensure all associated tenant roles are deleted
        tenantRoleRepository.findAllByTenantId(uuid).forEach(tenantRoleRepository::delete);

        getTenantUserAccounts(uuid).forEach(user -> removeUserAccountUseCase.execute(user.uuid(), tenant.getUuid()));

        tenantRepository.delete(tenant);
        eventPublisher.publish(new TenantDeletedEvent(uuid));
    }

    @Transactional
    public void addRoleToTenant(UUID tenantId, String roleName) {
        // Ensure tenant exists before adding a role
        // This check is important to prevent adding roles to non-existent tenants.
        tenantRepository
                .findByUuid(tenantId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with UUID: " + tenantId));

        boolean roleExists = tenantRoleRepository.findAllByTenantId(tenantId).stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));

        if (!roleExists) {
            TenantRole tenantRole = new TenantRole().setTenantId(tenantId).setRoleName(roleName);
            tenantRoleRepository.save(tenantRole);
        }
    }

    @Transactional
    public void removeRoleFromTenant(UUID tenantId, String roleName) {
        // Ensure tenant exists before attempting to remove a role
        tenantRepository
                .findByUuid(tenantId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with UUID: " + tenantId));

        tenantRoleRepository.findAllByTenantId(tenantId).stream()
                .filter(role -> role.getRoleName().equals(roleName))
                .findFirst()
                .ifPresent(tenantRoleRepository::delete);
    }

    private List<String> getTenantRoles(UUID tenantId) {
        // No need to check tenant existence here if called internally after existence is confirmed
        return tenantRoleRepository.findAllByTenantId(tenantId).stream()
                .map(TenantRole::getRoleName)
                .toList();
    }

    private TenantResponse mapToResponse(Tenant tenant, List<String> roles) {
        return new TenantResponse(
                tenant.getUuid(),
                tenant.getName(),
                Set.copyOf(roles),
                tenant.isActive(),
                roles.contains(DEMO_TENANT_ROLE));
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        List<String> tenantRoles = getTenantRoles(tenant.getUuid());
        return mapToResponse(tenant, tenantRoles);
    }
}
