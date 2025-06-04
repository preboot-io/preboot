package io.preboot.auth.test;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateInactiveUserAccountRequest;
import io.preboot.auth.api.dto.CreateTenantRequest;
import io.preboot.auth.api.dto.TenantResponse;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.TenantRoleRepository;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountSessionRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.core.service.TenantService;
import io.preboot.auth.core.spring.AuthAccountProperties;
import io.preboot.auth.test.eventhandler.AccountActivationEventHandler;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public class SuperAdminControllerTest extends AuthTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserAccountManagementApi userAccountManagementApi;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserAccountSessionRepository userAccountSessionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantRoleRepository tenantRoleRepository;

    @Autowired
    private UserAccountTenantRepository userAccountTenantRepository;

    @Autowired
    private AccountActivationEventHandler eventHandler;

    @MockBean
    private AuthAccountProperties authAccountProperties;

    private static final String SUPER_ADMIN_EMAIL = "super-admin@system.local";
    private static final String TENANT_USER_EMAIL = "tenant-user@example.com";
    private static final String PASSWORD = "password123";

    private UUID testTenantId;
    private UUID regularUserId;
    private String testTenantName = "Test Tenant";

    @BeforeEach
    void setUp() {
        TenantResponse tenant = tenantService.createTenant(new CreateTenantRequest(testTenantName, true));
        testTenantId = tenant.uuid();

        CreateInactiveUserAccountRequest request = new CreateInactiveUserAccountRequest(
                "regularuser", TENANT_USER_EMAIL, "en", "UTC", Set.of("USER"), Set.of(), testTenantId);

        var userInfo = userAccountManagementApi.createUserAccountForTenant(request);
        regularUserId = userInfo.uuid();

        String activationToken = eventHandler.getCapturedToken();
        try {
            mockMvc.perform(post("/api/auth/activation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    """
                                    {
                                        "token": "%s",
                                        "password": "%s"
                                    }
                                    """,
                                    activationToken, PASSWORD)))
                    .andExpect(status().isNoContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to activate test user account", e);
        }

        eventHandler.reset();

        String secondTenantName = "Second Test Tenant";
        TenantResponse secondTenant = tenantService.createTenant(new CreateTenantRequest(secondTenantName, true));
        UUID secondTenantId = secondTenant.uuid();

        CreateInactiveUserAccountRequest requestSecondTenant = new CreateInactiveUserAccountRequest(
                "seconduser", "second-tenant-user@example.com", "en", "UTC", Set.of("USER"), Set.of(), secondTenantId);

        userAccountManagementApi.createUserAccountForTenant(requestSecondTenant);

        activationToken = eventHandler.getCapturedToken();
        try {
            mockMvc.perform(post("/api/auth/activation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format(
                                    """
                                    {
                                        "token": "%s",
                                        "password": "%s"
                                    }
                                    """,
                                    activationToken, PASSWORD)))
                    .andExpect(status().isNoContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to activate second test user account", e);
        }

        eventHandler.reset();
    }

    @AfterEach
    void tearDown() {
        userAccountSessionRepository.deleteAll();
        userAccountTenantRepository.deleteAll();
        tenantRoleRepository.deleteAll();
        userAccountRepository.deleteAll();
        tenantRepository.deleteAll();
        eventHandler.reset();
    }

    // ===== SuperAdminTenantController Tests =====

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminSearchesTenants_thenReturnsTenants() throws Exception {
        mockMvc.perform(
                        post("/api/super-admin/tenants/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "page": 0,
                                    "size": 20,
                                    "sortField": "name",
                                    "sortDirection": "ASC",
                                    "filters": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[?(@.name == '" + testTenantName + "')]")
                        .exists());
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminGetsTenant_thenReturnsTenantDetails() throws Exception {
        mockMvc.perform(get("/api/super-admin/tenants/" + testTenantId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(testTenantId.toString()))
                .andExpect(jsonPath("$.name").value(testTenantName));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminCreatesTenant_thenTenantIsCreated() throws Exception {
        String newTenantName = "New Test Tenant";

        mockMvc.perform(post("/api/super-admin/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                        {
                            "name": "%s",
                            "active": true
                        }
                        """,
                                newTenantName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(newTenantName))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminUpdatesTenant_thenTenantIsUpdated() throws Exception {
        String updatedTenantName = "Updated Tenant Name";

        mockMvc.perform(put("/api/super-admin/tenants/" + testTenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                        {
                            "tenantId": "%s",
                            "name": "%s",
                            "active": false
                        }
                        """,
                                testTenantId, updatedTenantName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(testTenantId.toString()))
                .andExpect(jsonPath("$.name").value(updatedTenantName))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminDeletesTenant_thenTenantIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/super-admin/tenants/" + testTenantId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify tenant is deleted
        mockMvc.perform(get("/api/super-admin/tenants/" + testTenantId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminAddsRoleToTenant_thenRoleIsAdded() throws Exception {
        String roleName = "MANAGER";

        mockMvc.perform(post("/api/super-admin/tenants/" + testTenantId + "/roles/" + roleName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Verify role was added by checking tenant details
        mockMvc.perform(get("/api/super-admin/tenants/" + testTenantId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem(roleName)));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminRemovesRoleFromTenant_thenRoleIsRemoved() throws Exception {
        // First add a role
        String roleName = "TESTER";

        tenantService.addRoleToTenant(testTenantId, roleName);

        // Then remove it
        mockMvc.perform(delete("/api/super-admin/tenants/" + testTenantId + "/roles/" + roleName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify role was removed
        mockMvc.perform(get("/api/super-admin/tenants/" + testTenantId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", not(hasItem(roleName))));
    }

    @Test
    @WithMockUser(
            username = TENANT_USER_EMAIL,
            roles = {"USER"})
    void whenRegularUserAccessesSuperAdminTenantEndpoint_thenForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/super-admin/tenants/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "page": 0,
                            "size": 20
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    // ===== SuperAdminUserController Tests =====

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminSearchesAllUsers_thenReturnsUsers() throws Exception {
        mockMvc.perform(
                        post("/api/super-admin/users/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "page": 0,
                                    "size": 20,
                                    "sortField": "username",
                                    "sortDirection": "ASC",
                                    "filters": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[?(@.email == '" + TENANT_USER_EMAIL + "')]")
                        .exists());
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminCreatesUserForTenant_thenUserIsCreated() throws Exception {
        String newUserEmail = "new-tenant-user@example.com";

        mockMvc.perform(post("/api/super-admin/users/tenant/" + testTenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                        {
                            "username": "newtester",
                            "email": "%s",
                            "language": "en",
                            "timezone": "UTC",
                            "roles": ["TESTER"],
                            "permissions": []
                        }
                        """,
                                newUserEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(newUserEmail))
                .andExpect(jsonPath("$.tenantId").value(testTenantId.toString()))
                .andExpect(jsonPath("$.roles", hasItem("TESTER")));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminGetsUser_thenReturnsUserDetails() throws Exception {
        mockMvc.perform(get("/api/super-admin/users/" + regularUserId + "/tenant/" + testTenantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(regularUserId.toString()))
                .andExpect(jsonPath("$.email").value(TENANT_USER_EMAIL))
                .andExpect(jsonPath("$.tenantId").value(testTenantId.toString()));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminSearchesTenantUsers_thenReturnsUsers() throws Exception {
        mockMvc.perform(
                        post("/api/super-admin/users/tenant/" + testTenantId + "/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "page": 0,
                                    "size": 20,
                                    "sortField": "username",
                                    "sortDirection": "ASC",
                                    "filters": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.email == '" + TENANT_USER_EMAIL + "')]")
                        .exists());
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminRemovesUserFromTenant_thenUserIsRemoved() throws Exception {
        mockMvc.perform(delete("/api/super-admin/users/" + regularUserId + "/tenant/" + testTenantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify user is removed by trying to get them
        mockMvc.perform(get("/api/super-admin/users/" + regularUserId + "/tenant/" + testTenantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminAddsRoleToUser_thenRoleIsAdded() throws Exception {
        String roleName = "MANAGER";

        mockMvc.perform(post("/api/super-admin/users/" + regularUserId + "/tenant/" + testTenantId + "/roles/"
                                + roleName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItems("USER", "MANAGER")));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminRemovesRoleFromUser_thenRoleIsRemoved() throws Exception {
        String roleName = "USER";

        mockMvc.perform(delete("/api/super-admin/users/" + regularUserId + "/tenant/" + testTenantId + "/roles/"
                                + roleName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", not(hasItem("USER"))));
    }

    @Test
    @WithMockUser(
            username = TENANT_USER_EMAIL,
            roles = {"USER"})
    void whenRegularUserAccessesSuperAdminUserEndpoint_thenForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/super-admin/users/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "page": 0,
                            "size": 20
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminCreatesTenantWithDemoRoleEnabled_thenTenantHasDemoRole() throws Exception {
        when(authAccountProperties.isAssignDemoRoleToNewTenants()).thenReturn(true);

        String newTenantName = "Demo Enabled Tenant";

        mockMvc.perform(post("/api/super-admin/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                        {
                            "name": "%s",
                            "active": true
                        }
                        """,
                                newTenantName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(newTenantName))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles", hasItem("DEMO")));
    }

    @Test
    @WithMockUser(
            username = SUPER_ADMIN_EMAIL,
            roles = {"super-admin"})
    void whenSuperAdminCreatesTenantWithDemoRoleDisabled_thenTenantHasNoDemoRole() throws Exception {
        when(authAccountProperties.isAssignDemoRoleToNewTenants()).thenReturn(false);

        String newTenantName = "Demo Disabled Tenant";

        mockMvc.perform(post("/api/super-admin/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                        {
                            "name": "%s",
                            "active": true
                        }
                        """,
                                newTenantName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(newTenantName))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles", not(hasItem("DEMO"))));
    }
}
