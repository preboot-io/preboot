package io.preboot.files.s3;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class S3KeyUtilTest {

    private static final UUID TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID FILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Test
    void shouldGenerateValidKey() {
        String key = S3KeyUtil.generateKey(TENANT_ID, FILE_ID);

        assertThat(key).isEqualTo("files/550e8400-e29b-41d4-a716-446655440000/550e8400-e29b-41d4-a716-446655440001");
    }

    @Test
    void shouldParseValidKey() {
        String key = "files/550e8400-e29b-41d4-a716-446655440000/550e8400-e29b-41d4-a716-446655440001";

        S3KeyUtil.TenantFileIds ids = S3KeyUtil.parseKey(key);

        assertThat(ids.tenantId()).isEqualTo(TENANT_ID);
        assertThat(ids.fileId()).isEqualTo(FILE_ID);
    }

    @Test
    void shouldValidateValidKey() {
        String key = "files/550e8400-e29b-41d4-a716-446655440000/550e8400-e29b-41d4-a716-446655440001";

        assertThat(S3KeyUtil.isValidKey(key)).isTrue();
    }

    @Test
    void shouldRejectNullTenantId() {
        assertThatThrownBy(() -> S3KeyUtil.generateKey(null, FILE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId cannot be null");
    }

    @Test
    void shouldRejectNullFileId() {
        assertThatThrownBy(() -> S3KeyUtil.generateKey(TENANT_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fileId cannot be null");
    }

    @Test
    void shouldRejectNullKey() {
        assertThatThrownBy(() -> S3KeyUtil.parseKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("S3 key cannot be null or empty");
    }

    @Test
    void shouldRejectEmptyKey() {
        assertThatThrownBy(() -> S3KeyUtil.parseKey(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("S3 key cannot be null or empty");
    }

    @Test
    void shouldRejectInvalidKeyFormat() {
        assertThatThrownBy(() -> S3KeyUtil.parseKey("invalid/key/format"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid S3 key format");
    }

    @Test
    void shouldRejectInvalidUUIDInKey() {
        assertThatThrownBy(() -> S3KeyUtil.parseKey("files/invalid-uuid/550e8400-e29b-41d4-a716-446655440001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID format in S3 key");
    }

    @Test
    void shouldReturnFalseForInvalidKey() {
        assertThat(S3KeyUtil.isValidKey("invalid/key")).isFalse();
        assertThat(S3KeyUtil.isValidKey(null)).isFalse();
        assertThat(S3KeyUtil.isValidKey("")).isFalse();
    }

    @Test
    void shouldRoundTripGenerateAndParse() {
        String generatedKey = S3KeyUtil.generateKey(TENANT_ID, FILE_ID);
        S3KeyUtil.TenantFileIds parsedIds = S3KeyUtil.parseKey(generatedKey);

        assertThat(parsedIds.tenantId()).isEqualTo(TENANT_ID);
        assertThat(parsedIds.fileId()).isEqualTo(FILE_ID);
    }
}
