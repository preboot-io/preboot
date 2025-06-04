package io.preboot.auth.core.model;

import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account_permissions")
@Data
@Accessors(chain = true)
public class UserAccountPermission {
    private String name;
    private UUID tenantId;
}
