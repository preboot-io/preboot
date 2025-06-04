package io.preboot.auth.core.service;

import io.preboot.auth.api.exception.SessionExpiredException;
import io.preboot.auth.api.exception.SessionFingerprintException;
import io.preboot.auth.api.exception.SessionNotFoundException;
import io.preboot.auth.core.model.UserAccountSession;
import io.preboot.auth.core.repository.UserAccountSessionRepository;
import io.preboot.auth.core.spring.AuthSecurityProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final UserAccountSessionRepository sessionRepository;
    private final AuthSecurityProperties authSecurityProperties;

    @Transactional
    public UserAccountSession createSession(
            UUID userAccountId,
            UUID tenantId,
            String credentialType,
            String agent,
            String deviceFingerprint,
            String ip,
            boolean rememberMe) {

        Instant expiresAt = getExpiresAt(rememberMe);

        UserAccountSession session = new UserAccountSession()
                .setSessionId(UUID.randomUUID())
                .setUserAccountId(userAccountId)
                .setCredentialType(credentialType)
                .setAgent(agent)
                .setDeviceFingerprint(deviceFingerprint)
                .setIp(ip)
                .setCreatedAt(Instant.now())
                .setExpiresAt(expiresAt)
                .setRememberMe(rememberMe)
                .setTenantId(tenantId);

        return sessionRepository.save(session);
    }

    @Transactional
    public void deactivateSession(UUID sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setExpiresAt(Instant.now());
            sessionRepository.save(session);
        });
    }

    @Transactional
    public UserAccountSession refreshSession(UUID sessionId, String deviceFingerprint, UUID tenantId) {
        UserAccountSession session = getSession(sessionId);
        if (!session.getDeviceFingerprint().equals(deviceFingerprint)) {
            throw new SessionFingerprintException("Device fingerprint does not match");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new SessionExpiredException("Session has expired");
        }
        session.setExpiresAt(getExpiresAt(session.isRememberMe()));
        if (tenantId != null) {
            session.setTenantId(tenantId);
        }
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public UserAccountSession getSession(UUID sessionId) {
        return sessionRepository
                .findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));
    }

    @Transactional
    public void deleteExpiredSessions(Instant threshold) {
        sessionRepository.removeAllByExpiresAtBefore(threshold);
    }

    private Instant getExpiresAt(final boolean rememberMe) {
        return rememberMe
                ? Instant.now().plus(authSecurityProperties.getLongSessionTimeoutDays(), ChronoUnit.DAYS)
                : Instant.now().plus(authSecurityProperties.getSessionTimeoutMinutes(), ChronoUnit.MINUTES);
    }
}
