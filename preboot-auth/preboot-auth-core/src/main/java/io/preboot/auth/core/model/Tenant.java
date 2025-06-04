package io.preboot.auth.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Accessors(chain = true)
@Table("tenants")
public class Tenant {
    public static final UUID SUPER_ADMIN_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Id
    private Long id;

    private UUID uuid;
    private String name;
    private Instant createdAt;
    private Map<String, Object> attributes;
    private boolean active;

    public static Tenant createSuperAdminTenant() {
        Tenant tenant = new Tenant();
        tenant.setUuid(SUPER_ADMIN_TENANT);
        tenant.setName("Administration");
        tenant.setActive(true);
        return tenant;
    }
}
