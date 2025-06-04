package io.preboot.auth.core.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account_role_permissions")
@Data
public class UserAccountRolePermission {
    @Id
    private Long id;

    private String role;
    private String name;
}
