package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.CreateInactiveUserAccountRequest;
import io.preboot.auth.api.dto.CreateTenantAndInactiveUserAccountRequest;
import io.preboot.auth.api.dto.CreateTenantRequest;
import io.preboot.auth.api.dto.TenantResponse;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.core.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateTenantAndInactiveUserAccountUseCase {
    private final CreateInactiveUserAccountUseCase createInactiveUserAccountUseCase;
    private final TenantService tenantService;

    @Transactional
    public UserAccountInfo execute(final CreateTenantAndInactiveUserAccountRequest request) {
        final TenantResponse tenant = tenantService.createTenant(new CreateTenantRequest(request.tenantName(), true));
        return createInactiveUserAccountUseCase.execute(new CreateInactiveUserAccountRequest(
                request.username(),
                request.email(),
                request.language(),
                request.timezone(),
                request.roles(),
                request.permissions(),
                tenant.uuid()));
    }
}
