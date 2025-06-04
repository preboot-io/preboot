package io.preboot.auth.core.repository;

import io.preboot.auth.core.model.UserAccountSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountSessionRepository extends CrudRepository<UserAccountSession, Long> {
    Optional<UserAccountSession> findBySessionId(UUID sessionId);

    @Modifying
    @Query("DELETE FROM user_account_sessions WHERE user_account_id = :userAccountId AND tenant_id = :tenantId")
    void deleteByUserAccountIdAndTenantId(@Param("userAccountId") UUID userAccountId, @Param("tenantId") UUID tenantId);

    @Query("DELETE FROM user_account_sessions WHERE expires_at < :expiresAt")
    @Modifying
    void removeAllByExpiresAtBefore(@Param("expiresAt") Instant expiresAtBefore);
}
