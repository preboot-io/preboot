package io.preboot.auth.core.spring;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "preboot.security")
public class AuthSecurityProperties {
    private List<String> publicEndpoints = new ArrayList<>();
    private boolean enableCsrf = false;
    private String jwtSecret;
    private long sessionTimeoutMinutes = 15;
    private long longSessionTimeoutDays = 15;
    private long passwordResetTokenTimeoutInDays = 2;
    private long activationTokenTimeoutInDays = 30;
    List<String> corsAllowedOrigins = new ArrayList<>();
}
