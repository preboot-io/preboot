package io.preboot.auth.core.model;

import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_accounts_info_view")
@Data
public class UserAccountInfoView {
    @Id
    private Long id;

    private UUID uuid;
    private String username;
    private String email;
    private Boolean active;
    private String roles;
    private String permissions;
    private String tenantName;
    private UUID tenantId;
}
