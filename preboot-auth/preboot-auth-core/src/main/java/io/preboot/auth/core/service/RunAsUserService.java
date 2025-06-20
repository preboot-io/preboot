package io.preboot.auth.core.service;

import io.preboot.auth.api.SessionAwareAuthentication;
import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.UserAccountInfo;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunAsUserService {

    private final UserAccountManagementApi userAccountManagementApi;

    public void runAsUser(UUID userId, UUID tenantId, Runnable task) {
        UserAccountInfo userAccountInfo = userAccountManagementApi.getUserAccount(userId, tenantId);
        if (userAccountInfo == null) {
            throw new IllegalStateException(
                    String.format("User account not found for UserID: %s and TenantID: %s", userId, tenantId));
        }

        Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            SessionAwareAuthentication runAsAuth = createAuthentication(userAccountInfo);
            SecurityContextHolder.getContext().setAuthentication(runAsAuth);

            log.debug("Switched security context to user: {} (tenant: {})", userId, tenantId);
            task.run();

        } finally {
            if (originalAuth != null) {
                SecurityContextHolder.getContext().setAuthentication(originalAuth);
            } else {
                SecurityContextHolder.clearContext();
            }
            log.debug("Restored original security context");
        }
    }

    private SessionAwareAuthentication createAuthentication(UserAccountInfo userAccountInfo) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        userAccountInfo.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        userAccountInfo
                .getAllPermissions()
                .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        UUID taskSessionId = UUID.randomUUID();
        return new SessionAwareAuthentication(userAccountInfo, taskSessionId, authorities);
    }
}
