package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.TenantInfo;
import io.preboot.auth.core.model.Tenant;
import io.preboot.auth.core.model.UserAccountTenant;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserAccountTenantsUseCase {
    private final UserAccountTenantRepository userAccountTenantRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<TenantInfo> execute(final UUID userUUID) {
        return userAccountTenantRepository.findAllByUserAccountUuidOrderByLastUsedAt(userUUID).stream()
                .map(this::mapToDto)
                .toList();
    }

    private TenantInfo mapToDto(UserAccountTenant userAccountTenant) {
        final Tenant tenant =
                tenantRepository.findByUuid(userAccountTenant.getTenantUuid()).orElseThrow();
        return new TenantInfo(
                userAccountTenant.getTenantUuid(),
                tenant.getName(),
                userAccountTenant.getLastUsedAt(),
                tenant.isActive());
    }
}
