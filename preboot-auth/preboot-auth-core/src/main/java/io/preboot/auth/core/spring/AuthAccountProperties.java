package io.preboot.auth.core.spring;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "preboot.accounts")
public class AuthAccountProperties {
    private String defaultTimezone = "UTC";
    private String defaultLanguage = "en";
    private Set<String> defaultRoles = Set.of("ADMIN");
    private boolean registrationEnabled = true;
    private boolean assignDemoRoleToNewTenants = true;
}
