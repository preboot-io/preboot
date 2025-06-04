package io.preboot.auth.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.preboot.auth.core.service.JwtTokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class JwtTokenServiceTest extends AuthTestBase {

    @Autowired
    private JwtTokenService jwtTokenService;

    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID();
    }

    @Test
    void whenGenerateToken_thenCanExtractSessionId() {
        String token = jwtTokenService.generateToken(testSessionId);
        UUID extractedSessionId = jwtTokenService.extractSessionId(token);

        assertThat(extractedSessionId).isEqualTo(testSessionId);
    }

    @Test
    void whenTokenIsInvalid_thenThrowException() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> {
            jwtTokenService.extractSessionId(invalidToken);
        });
    }

    @Test
    void whenTokenIsModified_thenThrowException() {
        String token = jwtTokenService.generateToken(testSessionId);
        String modifiedToken = token + "modified";

        assertThrows(Exception.class, () -> {
            jwtTokenService.extractSessionId(modifiedToken);
        });
    }
}
