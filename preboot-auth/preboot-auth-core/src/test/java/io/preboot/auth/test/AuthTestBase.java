package io.preboot.auth.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all auth module tests that use PostgreSQL Testcontainers. All test classes should extend this class to
 * ensure consistent test setup.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(classes = {PostgresTestConfiguration.class, TestConfig.class})
public abstract class AuthTestBase {
    // Common test utilities can be added here
}
