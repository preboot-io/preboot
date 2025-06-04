package io.preboot.auth.core.rest;

import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateTenantAndInactiveUserAccountRequest;
import io.preboot.auth.core.spring.AuthAccountProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final UserAccountManagementApi userAccountManagementApi;
    private final AuthAccountProperties authAccountProperties;

    @PostMapping
    @Transactional
    public void register(@RequestBody RegisterTenantAndInactiveUserAccountRequest request) {
        if (!authAccountProperties.isRegistrationEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration disabled");
        }

        userAccountManagementApi.createTenantAndUserAccount(
                request.toCreateTenantAndInactiveUserAccountRequest(authAccountProperties));
    }

    public record RegisterTenantAndInactiveUserAccountRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            String language,
            String timezone,
            @NotBlank String tenantName) {
        public CreateTenantAndInactiveUserAccountRequest toCreateTenantAndInactiveUserAccountRequest(
                AuthAccountProperties authAccountProperties) {
            return new CreateTenantAndInactiveUserAccountRequest(
                    username, email, language, timezone, authAccountProperties.getDefaultRoles(), Set.of(), tenantName);
        }
    }
}
