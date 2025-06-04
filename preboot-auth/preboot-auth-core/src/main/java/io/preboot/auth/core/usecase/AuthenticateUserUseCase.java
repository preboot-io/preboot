package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.AuthResponse;
import io.preboot.auth.api.dto.PasswordLoginRequest;
import io.preboot.auth.api.exception.PasswordInvalidException;
import io.preboot.auth.api.exception.TenantAccessDeniedException;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.core.model.Tenant;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.model.UserAccountSession;
import io.preboot.auth.core.model.UserAccountTenant;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.core.service.DeviceFingerprintService;
import io.preboot.auth.core.service.JwtTokenService;
import io.preboot.auth.core.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {
    private final UserAccountRepository userAccountRepository;
    private final UserAccountTenantRepository userAccountTenantRepository;
    private final TenantRepository tenantRepository;
    private final SessionService sessionService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse execute(final PasswordLoginRequest request, final HttpServletRequest servletRequest) {
        final String userAgent = servletRequest.getHeader("User-Agent");
        final String ip = servletRequest.getHeader("X-Forwarded-For");

        // Validate credentials here
        final UserAccount userAccount = userAccountRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
        if (!passwordEncoder.matches(
                request.password(), userAccount.getEncodedPassword().orElseThrow(PasswordInvalidException::new))) {
            throw new PasswordInvalidException();
        }

        final String deviceFingerprint = deviceFingerprintService.generateFingerprint(servletRequest, null);
        final List<UserAccountTenant> tenantUsages =
                userAccountTenantRepository.findAllByUserAccountUuidOrderByLastUsedAt(userAccount.getUuid());

        if (tenantUsages.isEmpty() && userAccount.isTechnicalAdmin()) {
            return handleTechnicalAdmin(userAccount, userAgent, ip, deviceFingerprint, request.rememberMe());
        }

        if (tenantUsages.isEmpty()) {
            throw new TenantAccessDeniedException("User doesn't have access to any tenant");
        }

        UserAccountTenant selectedTenant = tenantUsages.stream()
                .filter(userAccountTenant -> {
                    Optional<Tenant> tenant = tenantRepository.findByUuid(userAccountTenant.getTenantUuid());
                    return tenant.isPresent() && tenant.get().isActive();
                })
                .findFirst()
                .orElseThrow(() -> new TenantAccessDeniedException("No active tenant found for this user"));

        UserAccountSession session = sessionService.createSession(
                userAccount.getUuid(),
                selectedTenant.getTenantUuid(),
                "PASSWORD",
                userAgent,
                deviceFingerprint,
                ip,
                request.rememberMe());

        userAccountTenantRepository.updateLastUsedAt(userAccount.getUuid(), selectedTenant.getTenantUuid());
        return new AuthResponse(jwtTokenService.generateToken(session.getSessionId()));
    }

    private AuthResponse handleTechnicalAdmin(
            UserAccount userAccount, String userAgent, String ip, String deviceFingerprint, boolean rememberMe) {
        UserAccountSession session = sessionService.createSession(
                userAccount.getUuid(),
                Tenant.SUPER_ADMIN_TENANT,
                "PASSWORD",
                userAgent,
                deviceFingerprint,
                ip,
                rememberMe);

        userAccountTenantRepository.updateLastUsedAt(userAccount.getUuid(), Tenant.SUPER_ADMIN_TENANT);
        return new AuthResponse(jwtTokenService.generateToken(session.getSessionId()));
    }
}
