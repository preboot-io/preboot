package io.preboot.files.s3;

import java.util.UUID;

public final class S3KeyUtil {

    private static final String KEY_PREFIX = "files";
    private static final String SEPARATOR = "/";

    private S3KeyUtil() {
        // Utility class
    }

    public static String generateKey(UUID tenantId, UUID fileId) {
        validateUUID(tenantId, "tenantId");
        validateUUID(fileId, "fileId");

        return KEY_PREFIX + SEPARATOR + tenantId + SEPARATOR + fileId;
    }

    public static TenantFileIds parseKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        String[] parts = key.split(SEPARATOR);
        if (parts.length != 3 || !KEY_PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException(
                    "Invalid S3 key format. Expected: files/{tenantId}/{fileId}, got: " + key);
        }

        try {
            UUID tenantId = UUID.fromString(parts[1]);
            UUID fileId = UUID.fromString(parts[2]);
            return new TenantFileIds(tenantId, fileId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format in S3 key: " + key, e);
        }
    }

    public static boolean isValidKey(String key) {
        try {
            parseKey(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void validateUUID(UUID uuid, String paramName) {
        if (uuid == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
    }

    public record TenantFileIds(UUID tenantId, UUID fileId) {}
}
