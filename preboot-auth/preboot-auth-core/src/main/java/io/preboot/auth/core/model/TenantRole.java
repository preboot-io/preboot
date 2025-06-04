package io.preboot.auth.core.model;

import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Accessors(chain = true)
@Table("tenant_roles")
public class TenantRole {
    @Id
    private Long id;

    private UUID tenantId;
    private String roleName;
}
