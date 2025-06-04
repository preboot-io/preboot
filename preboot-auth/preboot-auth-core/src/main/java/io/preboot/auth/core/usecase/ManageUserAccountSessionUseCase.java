package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.AuthResponse;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.SessionExpiredException;
import io.preboot.auth.core.model.UserAccountSession;
import io.preboot.auth.core.service.DeviceFingerprintService;
import io.preboot.auth.core.service.JwtTokenService;
import io.preboot.auth.core.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ManageUserAccountSessionUseCase {
    private final JwtTokenService jwtTokenService;
    private final SessionService sessionService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final GetUserAccountUseCase getUserAccountUseCase;

    @Transactional
    public void logout(final HttpServletRequest servletRequest) {
        String token = extractTokenFromRequest(servletRequest);
        UUID sessionId = jwtTokenService.extractSessionId(token);
        sessionService.deactivateSession(sessionId);
    }

    @Transactional(readOnly = true)
    public UserAccountInfo getCurrentUserAccount(final HttpServletRequest servletRequest) {
        String token = extractTokenFromRequest(servletRequest);
        UUID sessionId = jwtTokenService.extractSessionId(token);

        UserAccountSession session = sessionService.getSession(sessionId);
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new SessionExpiredException("Session has expired");
        }

        return getUserAccountUseCase.execute(session.getUserAccountId(), session.getTenantId());
    }

    @Transactional
    public AuthResponse refreshSession(final HttpServletRequest servletRequest, final UUID tenantUUID) {
        String token = extractTokenFromRequest(servletRequest);
        UUID sessionId = jwtTokenService.extractSessionId(token);
        final String deviceFingerprint = deviceFingerprintService.generateFingerprint(servletRequest, null);

        UserAccountSession newSession = sessionService.refreshSession(sessionId, deviceFingerprint, tenantUUID);
        String newToken = jwtTokenService.generateToken(newSession.getSessionId());

        return new AuthResponse(newToken);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}
