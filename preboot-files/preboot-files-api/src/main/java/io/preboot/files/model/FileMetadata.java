package io.preboot.files.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record FileMetadata(
        UUID fileId,
        String fileName,
        String contentType,
        long fileSize,
        UUID authorId,
        UUID tenantId,
        Instant createdAt,
        Instant lastModified,
        Map<String, String> customAttributes) {
    public static FileMetadata create(
            String fileName, String contentType, long fileSize, UUID authorId, UUID tenantId) {
        return new FileMetadata(
                UUID.randomUUID(),
                fileName,
                contentType,
                fileSize,
                authorId,
                tenantId,
                Instant.now(),
                Instant.now(),
                new HashMap<>());
    }
}
