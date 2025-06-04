package io.preboot.auth.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.preboot.auth.TestApplication;
import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateTenantAndInactiveUserAccountRequest;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountSessionRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.core.service.TenantService;
import io.preboot.auth.test.eventhandler.AccountActivationEventHandler;
import io.preboot.auth.test.utils.CreateUserAccountHelper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TenantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountManagementApi userAccountManagementApi;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserAccountSessionRepository userAccountSessionRepository;

    @Autowired
    private AccountActivationEventHandler eventHandler;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserAccountTenantRepository userAccountTenantRepository;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String FIRST_TENANT_NAME = "First Tenant";
    private static final String SECOND_TENANT_NAME = "Second Tenant";

    private String authToken;
    private CreateUserAccountHelper createUserAccountHelper;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        createUserAccountHelper = new CreateUserAccountHelper(mockMvc, eventHandler, userAccountManagementApi);
    }

    @AfterEach
    void tearDown() {
        userAccountSessionRepository.deleteAll();
        userAccountTenantRepository.deleteAll();
        userAccountRepository.deleteAll();
        tenantRepository.deleteAll();
        eventHandler.reset();
    }

    @Test
    void shouldCreateUserWithTwoTenantsAndSwitchBetweenThem() throws Exception {
        // Create first tenant and user
        CreateTenantAndInactiveUserAccountRequest firstTenantRequest = new CreateTenantAndInactiveUserAccountRequest(
                "testuser", TEST_EMAIL, "en", "UTC", Set.of("USER"), Set.of(), FIRST_TENANT_NAME);
        UserAccountInfo firstTenantUser = userAccountManagementApi.createTenantAndUserAccount(firstTenantRequest);

        // Activate the account
        String activationToken = eventHandler.getCapturedToken();
        mockMvc.perform(post("/api/auth/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "token": "%s",
                        "password": "%s"
                    }
                    """,
                                activationToken, TEST_PASSWORD)))
                .andExpect(status().isNoContent());

        // Login to get auth token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "email": "%s",
                        "password": "%s",
                        "rememberMe": false
                    }
                    """,
                                TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = JsonPath.read(loginResponse, "$.token");

        // Create second tenant for the same user
        CreateTenantAndInactiveUserAccountRequest secondTenantRequest = new CreateTenantAndInactiveUserAccountRequest(
                "testuser", TEST_EMAIL, "en", "UTC", Set.of("USER"), Set.of(), SECOND_TENANT_NAME);
        UserAccountInfo secondTenantUser = userAccountManagementApi.createTenantAndUserAccount(secondTenantRequest);

        // Get user's tenants
        String tenantsResponse = mockMvc.perform(
                        get("/api/auth/my-tenants").header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> tenantNames = JsonPath.read(tenantsResponse, "$[*].name");
        assertThat(tenantNames).containsExactlyInAnyOrder(FIRST_TENANT_NAME, SECOND_TENANT_NAME);

        // Switch to second tenant
        mockMvc.perform(post("/api/auth/use-tenant")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "tenantId": "%s"
                    }
                    """,
                                secondTenantUser.tenantId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // Verify current user info shows second tenant
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantName").value(SECOND_TENANT_NAME));
    }

    @Test
    void shouldFailToSwitchToNonExistentTenant() throws Exception {
        // Create user with one tenant
        createUserAccountHelper.createUserAccount(TEST_EMAIL, TEST_PASSWORD);

        // Login
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "email": "%s",
                        "password": "%s",
                        "rememberMe": false
                    }
                    """,
                                TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        // Attempt to switch to non-existent tenant
        mockMvc.perform(
                        post("/api/auth/use-tenant")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                        "tenantId": "00000000-0000-0000-0000-000000000000"
                    }
                    """))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    String message = result.getResolvedException().getMessage();
                    assertThat(message).contains("User does not have access to the specified tenant");
                });
    }

    @Test
    void shouldPreserveTenantRolesWhenSwitchingTenants() throws Exception {
        // Create first tenant with ADMIN role
        CreateTenantAndInactiveUserAccountRequest firstTenantRequest = new CreateTenantAndInactiveUserAccountRequest(
                "testuser", TEST_EMAIL, "en", "UTC", Set.of("ADMIN"), Set.of(), FIRST_TENANT_NAME);
        UserAccountInfo firstTenantUser = userAccountManagementApi.createTenantAndUserAccount(firstTenantRequest);

        // Activate the account
        String activationToken = eventHandler.getCapturedToken();
        mockMvc.perform(post("/api/auth/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "token": "%s",
                        "password": "%s"
                    }
                    """,
                                activationToken, TEST_PASSWORD)))
                .andExpect(status().isNoContent());

        // Login
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "email": "%s",
                        "password": "%s",
                        "rememberMe": false
                    }
                    """,
                                TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        // Create second tenant with USER role
        CreateTenantAndInactiveUserAccountRequest secondTenantRequest = new CreateTenantAndInactiveUserAccountRequest(
                "testuser", TEST_EMAIL, "en", "UTC", Set.of("USER"), Set.of(), SECOND_TENANT_NAME);
        UserAccountInfo secondTenantUser = userAccountManagementApi.createTenantAndUserAccount(secondTenantRequest);

        // Switch to second tenant
        mockMvc.perform(post("/api/auth/use-tenant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "tenantId": "%s"
                    }
                    """,
                                secondTenantUser.tenantId())))
                .andExpect(status().isOk());

        // Verify roles for second tenant
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.tenantName").value(SECOND_TENANT_NAME));

        // Switch back to first tenant
        mockMvc.perform(post("/api/auth/use-tenant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "tenantId": "%s"
                    }
                    """,
                                firstTenantUser.tenantId())))
                .andExpect(status().isOk());

        // Verify roles for first tenant
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.tenantName").value(FIRST_TENANT_NAME));
    }

    @Test
    void shouldAddTenantRolesToUserPermissions() throws Exception {
        // Create tenant with user
        CreateTenantAndInactiveUserAccountRequest request = new CreateTenantAndInactiveUserAccountRequest(
                "testuser", TEST_EMAIL, "en", "UTC", Set.of("USER"), Set.of(), "Test Tenant With Roles");
        UserAccountInfo userInfo = userAccountManagementApi.createTenantAndUserAccount(request);
        UUID tenantId = userInfo.tenantId();

        // Activate account
        String activationToken = eventHandler.getCapturedToken();
        mockMvc.perform(post("/api/auth/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "token": "%s",
                        "password": "%s"
                    }
                    """,
                                activationToken, TEST_PASSWORD)))
                .andExpect(status().isNoContent());

        // Add roles to tenant
        tenantService.addRoleToTenant(tenantId, "MANAGER");
        tenantService.addRoleToTenant(tenantId, "REPORTER");

        // Login to get auth token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                    {
                        "email": "%s",
                        "password": "%s",
                        "rememberMe": false
                    }
                    """,
                                TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        // Get current user info
        String userInfoResponse = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract permissions from response
        List<String> permissions = JsonPath.read(userInfoResponse, "$.permissions");

        // Verify that tenant role names are included in user permissions
        assertThat(permissions).contains("MANAGER", "REPORTER");

        // Verify user still has only USER role
        List<String> roles = JsonPath.read(userInfoResponse, "$.roles");
        assertThat(roles).containsExactly("USER");
    }
}
