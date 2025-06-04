package io.preboot.auth.api;

import java.time.Instant;

public interface UserAccountSessionManagementApi {
    void cleanExpiredSessions(Instant threshold);
}
