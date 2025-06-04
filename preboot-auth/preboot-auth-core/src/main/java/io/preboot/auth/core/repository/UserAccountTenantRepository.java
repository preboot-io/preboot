package io.preboot.auth.core.repository;

import io.preboot.auth.core.model.UserAccountTenant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountTenantRepository extends CrudRepository<UserAccountTenant, Long> {
    Optional<UserAccountTenant> getByUserAccountUuidAndTenantUuid(UUID userAccountUuid, UUID tenantUuid);

    @Modifying
    @Query("DELETE FROM user_account_tenants WHERE user_account_uuid = :userAccountUuid AND tenant_uuid = :tenantUuid")
    void deleteAllByUserAccountUuidAndTenantUuid(
            @Param("userAccountUuid") UUID userAccountUuid, @Param("tenantUuid") UUID tenantUuid);

    List<UserAccountTenant> findAllByTenantUuid(UUID tenantId);

    List<UserAccountTenant> findAllByUserAccountUuidOrderByLastUsedAt(UUID userAccountUuid);

    @Query(
            "UPDATE user_account_tenants SET last_used_at = now() WHERE user_account_uuid = :userAccountUuid AND tenant_uuid = :tenantUuid")
    @Modifying
    void updateLastUsedAt(@Param("userAccountUuid") UUID userAccountUuid, @Param("tenantUuid") UUID tenantUuid);
}
