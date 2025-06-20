package io.preboot.auth.core.service;

import io.preboot.auth.api.SessionAwareAuthentication;
import io.preboot.auth.api.resolver.TenantResolver;
import io.preboot.auth.core.model.UserAccountSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class SessionTenantResolver implements TenantResolver {
    private final SessionService sessionService;

    @Override
    public UUID getCurrentTenant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof SessionAwareAuthentication sessionAuth) {
            UUID sessionId = sessionAuth.getSessionId();
            UserAccountSession session = sessionService.getSession(sessionId);
            return session.getTenantId();
        }
        return null;
    }
}
