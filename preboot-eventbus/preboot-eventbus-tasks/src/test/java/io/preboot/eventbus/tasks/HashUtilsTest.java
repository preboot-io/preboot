package io.preboot.eventbus.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HashUtilsTest {

    @Test
    void shouldGenerateHashFromParameters() {
        assertThat(HashUtils.getHash(Map.of("key", "value"))).isNotBlank().isNotEqualTo("-");
    }

    @Test
    void hashShouldDependOnParameters() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "test");
        final String hash1 = HashUtils.getHash(map);

        map.put("key2", "test");
        final String hash2 = HashUtils.getHash(map); // same map, added new param

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void differentInsertionOrderShouldNotAffectHash() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("key1", "value1");
        map1.put("key2", "value2");

        Map<String, String> map2 = new HashMap<>();
        map2.put("key2", "value2");
        map2.put("key1", "value1");

        final String hash1 = HashUtils.getHash(map1);
        final String hash2 = HashUtils.getHash(map2);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void sameMapValuesShouldGenerateSameHashes() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("key1", "test");

        Map<String, String> map2 = new HashMap<>();
        map2.put("key1", "test");

        final String hash1 = HashUtils.getHash(map1);
        final String hash2 = HashUtils.getHash(map2);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void emptyMapHashConstantHash() {
        final String hash = HashUtils.getHash(Map.of());

        assertThat(hash).isEqualTo("-");
    }
}
