package io.preboot.auth.core.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Accessors(chain = true)
@Table("user_account_tenants")
public class UserAccountTenant {
    @Id
    private Long id;

    private UUID userAccountUuid;
    private UUID tenantUuid;
    private Instant lastUsedAt;
}
