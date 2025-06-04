package io.preboot.auth.core.rest;

import io.preboot.auth.api.dto.CreateTenantRequest;
import io.preboot.auth.api.dto.TenantResponse;
import io.preboot.auth.api.dto.TenantUpdateRequest;
import io.preboot.auth.api.guard.SuperAdminRoleAccessGuard;
import io.preboot.auth.core.service.TenantService;
import io.preboot.query.SearchParams;
import io.preboot.query.web.SearchRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** REST controller for super-admin to manage tenants. */
@RestController
@RequestMapping("/api/super-admin/tenants")
@RequiredArgsConstructor
@SuperAdminRoleAccessGuard
public class SuperAdminTenantController {

    private final TenantService tenantService;

    /** List all tenants with search criteria. */
    @PostMapping("/search")
    public Page<TenantResponse> searchTenants(@RequestBody @Valid SearchRequest request) {
        SearchParams params = SearchParams.builder()
                .page(request.page())
                .size(request.size())
                .sortField(request.sortField())
                .sortDirection(request.sortDirection())
                .filters(request.filters())
                .unpaged(request.unpaged())
                .build();

        return tenantService.getTenants(params);
    }

    /** Get a specific tenant by ID. */
    @GetMapping("/{tenantId}")
    public TenantResponse getTenant(@PathVariable UUID tenantId) {
        return tenantService.getTenant(tenantId);
    }

    /** Create a new tenant. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse createTenant(@RequestBody @Valid CreateTenantRequest request) {
        return tenantService.createTenant(request);
    }

    /** Update an existing tenant. */
    @PutMapping("/{tenantId}")
    public TenantResponse updateTenant(@PathVariable UUID tenantId, @RequestBody @Valid TenantUpdateRequest request) {
        return tenantService.updateTenant(tenantId, request);
    }

    /** Delete a tenant. */
    @DeleteMapping("/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenant(@PathVariable UUID tenantId) {
        tenantService.deleteTenant(tenantId);
    }

    /** Add a role to a tenant. */
    @PostMapping("/{tenantId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addRoleToTenant(@PathVariable UUID tenantId, @PathVariable String roleName) {
        tenantService.addRoleToTenant(tenantId, roleName);
    }

    /** Remove a role from a tenant. */
    @DeleteMapping("/{tenantId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRoleFromTenant(@PathVariable UUID tenantId, @PathVariable String roleName) {
        tenantService.removeRoleFromTenant(tenantId, roleName);
    }
}
