package io.preboot.auth.core.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account_sessions")
@Data
@Accessors(chain = true)
public class UserAccountSession {
    @Id
    private Long id;

    private UUID sessionId;
    private UUID userAccountId;
    private UUID impersonatedBy;
    private String credentialType;
    private String agent;
    private String ip;
    private String deviceFingerprint;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean rememberMe;
    private UUID tenantId;
}
