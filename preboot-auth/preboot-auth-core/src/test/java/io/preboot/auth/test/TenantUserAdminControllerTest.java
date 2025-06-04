package io.preboot.auth.test;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.api.dto.CreateInactiveUserAccountRequest;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.resolver.TenantResolver;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountSessionRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.core.spring.SessionAwareAuthentication;
import io.preboot.auth.test.eventhandler.AccountActivationEventHandler;
import io.preboot.auth.test.utils.CreateUserAccountHelper;
import io.preboot.query.SearchParams;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
public class TenantUserAdminControllerTest extends AuthTestBase {
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

    @MockBean
    private TenantResolver tenantResolver;

    private CreateUserAccountHelper createUserAccountHelper;
    private UUID tenantId;
    private UUID adminUserId;
    private UUID regularUserId;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String REGULAR_USER_EMAIL = "user@example.com";
    private static final String PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        createUserAccountHelper = new CreateUserAccountHelper(mockMvc, eventHandler, userAccountManagementApi);

        // Create admin user
        createUserAccountHelper.createUserAccount(ADMIN_EMAIL, PASSWORD);
        tenantId = createUserAccountHelper.getTenantId();

        // Mock the TenantResolver to return our test tenant ID
        when(tenantResolver.getCurrentTenant()).thenReturn(tenantId);

        // Create regular user
        CreateInactiveUserAccountRequest request = new CreateInactiveUserAccountRequest(
                "regularuser", REGULAR_USER_EMAIL, "en", "UTC", Set.of("USER"), Set.of(), tenantId);
        var userInfo = userAccountManagementApi.createUserAccountForTenant(request);
        regularUserId = userInfo.uuid();

        // Activate the regular user account
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
            throw new RuntimeException("Failed to activate regular user account", e);
        }

        // Store the admin user ID for tests
        try {
            var adminUserInfo =
                    userAccountManagementApi.getUserAccountsInfo(SearchParams.empty(), tenantId).getContent().stream()
                            .filter(user -> user.email().equals(ADMIN_EMAIL))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Admin user not found"));
            adminUserId = adminUserInfo.uuid();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get admin user ID", e);
        }
    }

    @AfterEach
    void tearDown() {
        userAccountSessionRepository.deleteAll();
        userAccountTenantRepository.deleteAll();
        userAccountRepository.deleteAll();
        tenantRepository.deleteAll();
        eventHandler.reset();
    }

    /** Create a RequestPostProcessor that sets up authentication for the admin user */
    private RequestPostProcessor withAdminUser() {
        // Create the UserAccountInfo for this user
        UserAccountInfo adminUserInfo = new UserAccountInfo(
                adminUserId,
                "admin",
                ADMIN_EMAIL,
                Set.of("ADMIN"),
                new HashSet<>(),
                new HashSet<>(),
                true,
                tenantId,
                "Test Tenant");

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // Create the SessionAwareAuthentication
        SessionAwareAuthentication auth = new SessionAwareAuthentication(adminUserInfo, UUID.randomUUID(), authorities);

        // Use Spring Security's authentication processor
        return authentication(auth);
    }

    /** Create a RequestPostProcessor that sets up authentication for a regular user */
    private RequestPostProcessor withRegularUser() {
        // Create the UserAccountInfo for this user
        UserAccountInfo userInfo = new UserAccountInfo(
                regularUserId,
                "regularuser",
                REGULAR_USER_EMAIL,
                Set.of("USER"),
                new HashSet<>(),
                new HashSet<>(),
                true,
                tenantId,
                "Test Tenant");

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Create the SessionAwareAuthentication
        SessionAwareAuthentication auth = new SessionAwareAuthentication(userInfo, UUID.randomUUID(), authorities);

        // Use Spring Security's authentication processor
        return authentication(auth);
    }

    @Test
    void whenAdminFiltersUsersByUsername_thenReturnsMatchingUsers() throws Exception {
        mockMvc.perform(
                        post("/api/tenant/users/search")
                                .with(withAdminUser())
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
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void whenAdminListsAllUsers_thenReturnsAllUsersInTenant() throws Exception {
        mockMvc.perform(
                        post("/api/tenant/users/search")
                                .with(withAdminUser())
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
                .andExpect(jsonPath("$.content[?(@.email == '" + ADMIN_EMAIL + "')]")
                        .exists())
                .andExpect(jsonPath("$.content[?(@.email == '" + REGULAR_USER_EMAIL + "')]")
                        .exists());
    }

    @Test
    void whenAdminGetsUser_thenReturnsUserDetails() throws Exception {
        mockMvc.perform(get("/api/tenant/users/" + regularUserId)
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(regularUserId.toString()))
                .andExpect(jsonPath("$.email").value(REGULAR_USER_EMAIL))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void whenAdminCreatesUser_thenUserIsCreated() throws Exception {
        String newUserEmail = "newuser@example.com";
        mockMvc.perform(post("/api/tenant/users")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                """
                                {
                                    "username": "newuser",
                                    "email": "%s",
                                    "language": "en",
                                    "timezone": "UTC",
                                    "roles": ["USER", "REPORTER"],
                                    "permissions": []
                                }
                                """,
                                newUserEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(newUserEmail))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasItems("USER", "REPORTER")));
    }

    @Test
    void whenAdminUpdatesUserRoles_thenRolesAreUpdated() throws Exception {
        mockMvc.perform(put("/api/tenant/users/" + regularUserId + "/roles")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {
                            "userUuid": "%s",
                            "roles": ["MANAGER", "REPORTER"]
                        }
                        """
                                        .formatted(regularUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasItems("MANAGER", "REPORTER")))
                .andExpect(jsonPath("$.roles", not(hasItem("USER"))));
    }

    @Test
    void whenAdminAddsRoleToUser_thenRoleIsAdded() throws Exception {
        mockMvc.perform(post("/api/tenant/users/" + regularUserId + "/roles/MANAGER")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasItems("USER", "MANAGER")));
    }

    @Test
    void whenAdminRemovesRoleFromUser_thenRoleIsRemoved() throws Exception {
        mockMvc.perform(delete("/api/tenant/users/" + regularUserId + "/roles/USER")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", not(hasItem("USER"))));
    }

    @Test
    void whenRegularUserAccessesAdminEndpoint_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/tenant/users/search")
                        .with(withRegularUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\": 0, \"size\": 20}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenAdminRemovesOwnAdminRole_thenForbidden() throws Exception {
        mockMvc.perform(put("/api/tenant/users/" + adminUserId + "/roles")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {
                            "userUuid": "%s",
                            "roles": ["USER", "REPORTER"]
                        }
                        """
                                        .formatted(adminUserId)))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    String message = result.getResolvedException().getMessage();
                    org.junit.jupiter.api.Assertions.assertTrue(message.contains("Cannot remove your own ADMIN role"));
                });
    }

    @Test
    void whenAdminRemovesOwnAdminRoleSpecifically_thenForbidden() throws Exception {
        mockMvc.perform(delete("/api/tenant/users/" + adminUserId + "/roles/ADMIN")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    String message = result.getResolvedException().getMessage();
                    org.junit.jupiter.api.Assertions.assertTrue(message.contains("Cannot remove your own ADMIN role"));
                });
    }

    @Test
    void whenAdminRemovesSelf_thenForbidden() throws Exception {
        mockMvc.perform(delete("/api/tenant/users/" + adminUserId)
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    String message = result.getResolvedException().getMessage();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            message.contains("Cannot remove yourself from the tenant"));
                });
    }

    @Test
    void whenAdminRemovesUser_thenUserIsRemoved() throws Exception {
        mockMvc.perform(delete("/api/tenant/users/" + regularUserId)
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify user is removed
        mockMvc.perform(get("/api/tenant/users/" + regularUserId)
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenAdminUpdatesOwnRolesButKeepsAdmin_thenSucceeds() throws Exception {
        mockMvc.perform(put("/api/tenant/users/" + adminUserId + "/roles")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {
                            "userUuid": "%s",
                            "roles": ["ADMIN", "REPORTER", "VIEWER"]
                        }
                        """
                                        .formatted(adminUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItems("ADMIN", "REPORTER", "VIEWER")));
    }

    @Test
    void whenAdminRemovesOtherAdminRole_thenSucceeds() throws Exception {
        // Create another admin user
        CreateInactiveUserAccountRequest request = new CreateInactiveUserAccountRequest(
                "anotheradmin", "another-admin@example.com", "en", "UTC", Set.of("ADMIN"), Set.of(), tenantId);
        var anotherAdminInfo = userAccountManagementApi.createUserAccountForTenant(request);

        // Activate the other admin account
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
                                activationToken, PASSWORD)))
                .andExpect(status().isNoContent());

        // Admin should be able to remove admin role from another admin
        mockMvc.perform(delete("/api/tenant/users/" + anotherAdminInfo.uuid() + "/roles/ADMIN")
                        .with(withAdminUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", not(hasItem("ADMIN"))));
    }
}
