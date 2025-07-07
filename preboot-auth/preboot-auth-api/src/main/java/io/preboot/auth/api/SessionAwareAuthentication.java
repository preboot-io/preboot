package io.preboot.auth.api;

import io.preboot.auth.api.dto.UserAccountInfo;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class SessionAwareAuthentication extends UsernamePasswordAuthenticationToken {
    private final UUID sessionId;

    public SessionAwareAuthentication(
            UserAccountInfo principal, UUID sessionId, Collection<? extends GrantedAuthority> authorities) {
        super(principal, null, authorities);
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
