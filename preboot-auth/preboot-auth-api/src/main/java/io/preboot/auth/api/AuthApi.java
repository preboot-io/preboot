package io.preboot.auth.api;

import io.preboot.auth.api.dto.AuthResponse;
import io.preboot.auth.api.dto.PasswordLoginRequest;
import io.preboot.auth.api.dto.TenantInfo;
import io.preboot.auth.api.dto.UserAccountInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

public interface AuthApi {
    AuthResponse login(PasswordLoginRequest request, HttpServletRequest servletRequest);

    void logout(final HttpServletRequest servletRequest);

    AuthResponse refreshSession(final HttpServletRequest servletRequest);

    UserAccountInfo getCurrentUserAccount(final HttpServletRequest servletRequest);

    List<TenantInfo> getCurrentUserTenants(HttpServletRequest servletRequest);

    AuthResponse setCurrentUserTenant(UUID tenantUUID, HttpServletRequest servletRequest);
}
