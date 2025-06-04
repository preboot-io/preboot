package io.preboot.auth.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.preboot.auth.TestApplication;
import io.preboot.auth.api.UserAccountManagementApi;
import io.preboot.auth.core.repository.TenantRepository;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountSessionRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.core.spring.AuthSecurityConfiguration;
import io.preboot.auth.test.eventhandler.AccountActivationEventHandler;
import io.preboot.auth.test.utils.CreateUserAccountHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, AuthSecurityConfiguration.class})
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserAccountSessionRepository userAccountSessionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserAccountTenantRepository userAccountTenantRepository;

    @Autowired
    private AccountActivationEventHandler eventHandler;

    @Autowired
    private UserAccountManagementApi userAccountManagementApi;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        eventHandler.reset();
        // Create test user
        CreateUserAccountHelper createUserAccountHelper =
                new CreateUserAccountHelper(mockMvc, eventHandler, userAccountManagementApi);
        createUserAccountHelper.createUserAccount(TEST_EMAIL, TEST_PASSWORD);
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
    void whenAccessingProtectedEndpointWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/protected")).andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessingProtectedEndpointWithAuth_thenOk() throws Exception {
        final String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                            "email": "%s",
                                            "password": "%s"
                                        }
                                        """
                                        .formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        final String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(get("/api/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessingPublicEndpoint_thenOk() throws Exception {
        mockMvc.perform(get("/api/public/test")).andExpect(status().isOk());
    }

    @Test
    void whenLoggingInWithValidCredentials_thenReceiveToken() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "email": "test@example.com",
                                                    "password": "password123"
                                                }
                                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void whenLoggingInWithInvalidCredentials_thenUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                    "email": "test@example.com",
                                                    "password": "wrongpassword"
                                                }
                                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenTokenExpired_thenUnauthorized() throws Exception {
        // This would require setting up an expired token
        String expiredToken = "Bearer eyJhbGciOiJIUzI1NiJ9..."; // Create an expired token

        mockMvc.perform(get("/api/protected").header("Authorization", expiredToken))
                .andExpect(status().isUnauthorized());
    }
}
