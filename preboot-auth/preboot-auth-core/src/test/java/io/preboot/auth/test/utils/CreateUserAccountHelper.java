package io.preboot.auth.test.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateTenantAndInactiveUserAccountRequest;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.test.eventhandler.AccountActivationEventHandler;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor
public class CreateUserAccountHelper {
    private final MockMvc mockMvc;
    private final AccountActivationEventHandler eventHandler;
    private final UserAccountManagementApi userAccountManagementApi;

    @Getter
    private UUID tenantId;

    @SneakyThrows
    public void createUserAccount(String email, String password) {
        CreateTenantAndInactiveUserAccountRequest request = new CreateTenantAndInactiveUserAccountRequest(
                "newuser", email, "en", "UTC", Set.of("USER"), Set.of(), "tenant");
        final UserAccountInfo tenantAndUserAccount = userAccountManagementApi.createTenantAndUserAccount(request);
        this.tenantId = tenantAndUserAccount.tenantId();

        String activationToken = eventHandler.getCapturedToken();
        assertThat(activationToken).isNotNull();

        // Activate account with password
        mockMvc.perform(post("/api/auth/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "token": "%s",
                                    "password": "%s"
                                }
                                """
                                        .formatted(activationToken, password)))
                .andExpect(status().isNoContent());
    }
}
