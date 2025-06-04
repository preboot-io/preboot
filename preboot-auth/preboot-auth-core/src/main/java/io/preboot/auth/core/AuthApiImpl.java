package io.preboot.auth.core;

import io.preboot.auth.api.AuthApi;
import io.preboot.auth.api.dto.AuthResponse;
import io.preboot.auth.api.dto.PasswordLoginRequest;
import io.preboot.auth.api.dto.TenantInfo;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.TenantAccessDeniedException;
import io.preboot.auth.api.exception.TenantInactiveException;
import io.preboot.auth.core.usecase.AuthenticateUserUseCase;
import io.preboot.auth.core.usecase.GetUserAccountTenantsUseCase;
import io.preboot.auth.core.usecase.ManageUserAccountSessionUseCase;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AuthApiImpl implements AuthApi {
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final ManageUserAccountSessionUseCase manageUserAccountSessionUseCase;
    private final GetUserAccountTenantsUseCase getUserAccountTenantsUseCase;

    @Override
    public AuthResponse login(final PasswordLoginRequest request, final HttpServletRequest httpServletRequest) {
        return authenticateUserUseCase.execute(request, httpServletRequest);
    }

    @Override
    public void logout(final HttpServletRequest httpServletRequest) {
        manageUserAccountSessionUseCase.logout(httpServletRequest);
    }

    @Override
    public AuthResponse refreshSession(final HttpServletRequest httpServletRequest) {
        return manageUserAccountSessionUseCase.refreshSession(httpServletRequest, null);
    }

    @Override
    public UserAccountInfo getCurrentUserAccount(final HttpServletRequest servletRequest) {
        return manageUserAccountSessionUseCase.getCurrentUserAccount(servletRequest);
    }

    @Override
    public List<TenantInfo> getCurrentUserTenants(final HttpServletRequest servletRequest) {
        final UserAccountInfo currentUserAccount =
                manageUserAccountSessionUseCase.getCurrentUserAccount(servletRequest);
        return getUserAccountTenantsUseCase.execute(currentUserAccount.uuid());
    }

    @Override
    @Transactional
    public AuthResponse setCurrentUserTenant(final UUID tenantUUID, final HttpServletRequest servletRequest) {
        final UserAccountInfo currentUserAccount =
                manageUserAccountSessionUseCase.getCurrentUserAccount(servletRequest);
        final List<TenantInfo> userTenants = getUserAccountTenantsUseCase.execute(currentUserAccount.uuid());

        Optional<TenantInfo> tenantOptional = userTenants.stream()
                .filter(tenant -> tenant.uuid().equals(tenantUUID))
                .findFirst();

        if (tenantOptional.isEmpty()) {
            throw new TenantAccessDeniedException("User does not have access to the specified tenant");
        } else if (!tenantOptional.get().active()) {
            throw new TenantInactiveException("Specified tenant is not active");
        }

        return manageUserAccountSessionUseCase.refreshSession(servletRequest, tenantUUID);
    }
}
