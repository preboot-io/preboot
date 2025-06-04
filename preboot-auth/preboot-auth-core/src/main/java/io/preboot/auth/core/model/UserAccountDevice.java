package io.preboot.auth.core.model;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account_devices")
@Data
public class UserAccountDevice {
    private String name;
    private String deviceFingerprint;
    private Instant createdAt;
}
