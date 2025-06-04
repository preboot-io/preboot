package io.preboot.auth.core;

import io.preboot.auth.api.UserAccountSessionManagementApi;
import io.preboot.auth.core.service.SessionService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserAccountSessionManagementApiImpl implements UserAccountSessionManagementApi {
    private final SessionService sessionService;

    @Override
    public void cleanExpiredSessions(final Instant threshold) {
        sessionService.deleteExpiredSessions(threshold);
    }
}
