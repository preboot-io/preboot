package io.preboot.files.s3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.preboot.files.model.FileMetadata;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
public final class S3MetadataMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // S3 metadata keys (must be lowercase and alphanumeric)
    private static final String FILE_ID_KEY = "preboot-file-id";
    private static final String FILE_NAME_KEY = "preboot-file-name";
    private static final String CONTENT_TYPE_KEY = "preboot-content-type";
    private static final String FILE_SIZE_KEY = "preboot-file-size";
    private static final String AUTHOR_ID_KEY = "preboot-author-id";
    private static final String TENANT_ID_KEY = "preboot-tenant-id";
    private static final String CREATED_AT_KEY = "preboot-created-at";
    private static final String LAST_MODIFIED_KEY = "preboot-last-modified";
    private static final String CUSTOM_ATTRIBUTES_KEY = "preboot-custom-attributes";

    private S3MetadataMapper() {
        // Utility class
    }

    public static PutObjectRequest.Builder addMetadata(PutObjectRequest.Builder builder, FileMetadata metadata) {
        Map<String, String> s3Metadata = new HashMap<>();

        s3Metadata.put(FILE_ID_KEY, metadata.fileId().toString());
        s3Metadata.put(FILE_NAME_KEY, metadata.fileName());
        s3Metadata.put(CONTENT_TYPE_KEY, metadata.contentType());
        s3Metadata.put(FILE_SIZE_KEY, String.valueOf(metadata.fileSize()));
        s3Metadata.put(AUTHOR_ID_KEY, metadata.authorId().toString());
        s3Metadata.put(TENANT_ID_KEY, metadata.tenantId().toString());
        s3Metadata.put(CREATED_AT_KEY, metadata.createdAt().toString());
        s3Metadata.put(LAST_MODIFIED_KEY, metadata.lastModified().toString());

        // Serialize custom attributes to JSON
        if (metadata.customAttributes() != null && !metadata.customAttributes().isEmpty()) {
            try {
                String customAttributesJson = OBJECT_MAPPER.writeValueAsString(metadata.customAttributes());
                s3Metadata.put(CUSTOM_ATTRIBUTES_KEY, customAttributesJson);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize custom attributes for file {}: {}", metadata.fileId(), e.getMessage());
            }
        }

        return builder.metadata(s3Metadata);
    }

    public static FileMetadata extractMetadata(GetObjectResponse response, String s3Key) {
        Map<String, String> s3Metadata = response.metadata();
        S3KeyUtil.TenantFileIds ids = S3KeyUtil.parseKey(s3Key);

        try {
            String fileName = s3Metadata.get(FILE_NAME_KEY);
            String contentType = s3Metadata.get(CONTENT_TYPE_KEY);
            long fileSize = Long.parseLong(s3Metadata.get(FILE_SIZE_KEY));
            UUID authorId = UUID.fromString(s3Metadata.get(AUTHOR_ID_KEY));
            Instant createdAt = Instant.parse(s3Metadata.get(CREATED_AT_KEY));
            Instant lastModified = Instant.parse(s3Metadata.get(LAST_MODIFIED_KEY));

            // Deserialize custom attributes from JSON
            Map<String, String> customAttributes = new HashMap<>();
            String customAttributesJson = s3Metadata.get(CUSTOM_ATTRIBUTES_KEY);
            if (customAttributesJson != null && !customAttributesJson.trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> deserializedAttributes =
                            OBJECT_MAPPER.readValue(customAttributesJson, Map.class);
                    customAttributes.putAll(deserializedAttributes);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize custom attributes for file {}: {}", ids.fileId(), e.getMessage());
                }
            }

            return new FileMetadata(
                    ids.fileId(),
                    fileName,
                    contentType,
                    fileSize,
                    authorId,
                    ids.tenantId(),
                    createdAt,
                    lastModified,
                    customAttributes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract metadata from S3 object: " + s3Key, e);
        }
    }

    public static FileMetadata createMetadataWithS3Info(
            GetObjectResponse response,
            String s3Key,
            String fileName,
            String contentType,
            UUID authorId,
            UUID tenantId) {
        S3KeyUtil.TenantFileIds ids = S3KeyUtil.parseKey(s3Key);

        return new FileMetadata(
                ids.fileId(),
                fileName,
                contentType,
                response.contentLength(),
                authorId,
                ids.tenantId(),
                response.lastModified(),
                response.lastModified(),
                new HashMap<>());
    }
}
