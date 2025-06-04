package io.preboot.auth.core.repository;

import io.preboot.auth.core.model.TenantRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRoleRepository extends CrudRepository<TenantRole, Long> {
    List<TenantRole> findAllByTenantId(UUID tenantId);
}
