package io.preboot.auth.core.repository;

import io.preboot.auth.core.model.UserAccountRolePermission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountRolePermissionRepository extends CrudRepository<UserAccountRolePermission, Long> {
    UserAccountRolePermission findByRole(String role);

    List<UserAccountRolePermission> findAllByRoleIn(Collection<String> roles);
}
