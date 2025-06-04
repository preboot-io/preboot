package io.preboot.auth.core.model;

import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account_roles")
@Data
@Accessors(chain = true)
public class UserAccountRole {
    private String name;
    private UUID tenantId;
}
