package io.preboot.auth.test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateTenantAndInactiveUserAccountRequest;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountSessionRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.test.eventhandler.PasswordResetEventHandler;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountManagementApi userAccountManagementApi;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserAccountSessionRepository userAccountSessionRepository;

    @Autowired
    private PasswordResetEventHandler eventHandler;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserAccountTenantRepository userAccountTenantRepository;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String NEW_PASSWORD = "newPassword123";

    @BeforeEach
    void setUp() {
        // Create test user
        CreateTenantAndInactiveUserAccountRequest createRequest = new CreateTenantAndInactiveUserAccountRequest(
                "testuser", TEST_EMAIL, "en", "UTC", Set.of("USER"), Set.of(), "tenant");

        userAccountManagementApi.createTenantAndUserAccount(createRequest);
    }

    @AfterEach
    void afterEach() {
        userAccountSessionRepository.deleteAll();
        userAccountTenantRepository.deleteAll();
        userAccountRepository.deleteAll();
        tenantRepository.deleteAll();
        eventHandler.reset();
    }

    @Test
    void whenRequestingPasswordReset_thenTokenIsGeneratedAndCanBeUsedToResetPassword() throws Exception {
        // Request password reset
        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "email": "%s"
                                }
                                """
                                        .formatted(TEST_EMAIL)))
                .andExpect(status().isNoContent());

        // Verify event was captured
        assertThat(eventHandler.getCapturedToken()).isNotNull();

        // Reset password using token
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "token": "%s",
                                    "newPassword": "%s"
                                }
                                """
                                        .formatted(eventHandler.getCapturedToken(), NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // Verify can login with new password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "email": "%s",
                                    "password": "%s"
                                }
                                """
                                        .formatted(TEST_EMAIL, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void whenRequestingPasswordReset_withNonExistentEmail_thenNotFound() throws Exception {
        mockMvc.perform(
                        post("/api/auth/password/reset-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "email": "nonexistent@example.com"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenResettingPassword_withInvalidToken_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "token": "invalid-token",
                                    "newPassword": "%s"
                                }
                                """
                                        .formatted(NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenResettingPassword_withUsedToken_thenUnauthorized() throws Exception {
        // Request first reset
        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "email": "%s"
                                }
                                """
                                        .formatted(TEST_EMAIL)))
                .andExpect(status().isNoContent());

        String firstToken = eventHandler.getCapturedToken();

        // Use the token
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "token": "%s",
                                    "newPassword": "%s"
                                }
                                """
                                        .formatted(firstToken, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // Try to use the same token again
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "token": "%s",
                                    "newPassword": "anotherPassword123"
                                }
                                """
                                        .formatted(firstToken)))
                .andExpect(status().isUnauthorized());
    }
}
