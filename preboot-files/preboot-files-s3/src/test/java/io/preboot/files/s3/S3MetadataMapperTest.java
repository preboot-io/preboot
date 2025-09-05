package io.preboot.files.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.preboot.files.model.FileMetadata;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3MetadataMapperTest {

    private static final UUID TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID FILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String S3_KEY =
            "files/550e8400-e29b-41d4-a716-446655440000/550e8400-e29b-41d4-a716-446655440001";
    private static final Instant NOW = Instant.parse("2023-01-01T10:00:00Z");

    @Test
    void shouldAddMetadataToPutRequest() {
        Map<String, String> customAttributes = Map.of("category", "document", "priority", "high");

        FileMetadata metadata = new FileMetadata(
                FILE_ID, "test.pdf", "application/pdf", 1024L, AUTHOR_ID, TENANT_ID, NOW, NOW, customAttributes);

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        PutObjectRequest.Builder result = S3MetadataMapper.addMetadata(builder, metadata);

        PutObjectRequest request = result.build();
        Map<String, String> s3Metadata = request.metadata();

        assertThat(s3Metadata).containsEntry("preboot-file-id", FILE_ID.toString());
        assertThat(s3Metadata).containsEntry("preboot-file-name", "test.pdf");
        assertThat(s3Metadata).containsEntry("preboot-content-type", "application/pdf");
        assertThat(s3Metadata).containsEntry("preboot-file-size", "1024");
        assertThat(s3Metadata).containsEntry("preboot-author-id", AUTHOR_ID.toString());
        assertThat(s3Metadata).containsEntry("preboot-tenant-id", TENANT_ID.toString());
        assertThat(s3Metadata).containsEntry("preboot-created-at", NOW.toString());
        assertThat(s3Metadata).containsEntry("preboot-last-modified", NOW.toString());
        assertThat(s3Metadata).containsKey("preboot-custom-attributes");
    }

    @Test
    void shouldExtractMetadataFromGetResponse() {
        Map<String, String> s3Metadata = new HashMap<>();
        s3Metadata.put("preboot-file-id", FILE_ID.toString());
        s3Metadata.put("preboot-file-name", "test.pdf");
        s3Metadata.put("preboot-content-type", "application/pdf");
        s3Metadata.put("preboot-file-size", "1024");
        s3Metadata.put("preboot-author-id", AUTHOR_ID.toString());
        s3Metadata.put("preboot-tenant-id", TENANT_ID.toString());
        s3Metadata.put("preboot-created-at", NOW.toString());
        s3Metadata.put("preboot-last-modified", NOW.toString());
        s3Metadata.put("preboot-custom-attributes", "{\"category\":\"document\",\"priority\":\"high\"}");

        GetObjectResponse response =
                GetObjectResponse.builder().metadata(s3Metadata).build();

        FileMetadata metadata = S3MetadataMapper.extractMetadata(response, S3_KEY);

        assertThat(metadata.fileId()).isEqualTo(FILE_ID);
        assertThat(metadata.fileName()).isEqualTo("test.pdf");
        assertThat(metadata.contentType()).isEqualTo("application/pdf");
        assertThat(metadata.fileSize()).isEqualTo(1024L);
        assertThat(metadata.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(metadata.tenantId()).isEqualTo(TENANT_ID);
        assertThat(metadata.createdAt()).isEqualTo(NOW);
        assertThat(metadata.lastModified()).isEqualTo(NOW);
        assertThat(metadata.customAttributes())
                .containsEntry("category", "document")
                .containsEntry("priority", "high");
    }

    @Test
    void shouldHandleEmptyCustomAttributes() {
        FileMetadata metadata = new FileMetadata(
                FILE_ID, "test.pdf", "application/pdf", 1024L, AUTHOR_ID, TENANT_ID, NOW, NOW, new HashMap<>());

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        PutObjectRequest.Builder result = S3MetadataMapper.addMetadata(builder, metadata);

        PutObjectRequest request = result.build();
        Map<String, String> s3Metadata = request.metadata();

        assertThat(s3Metadata).doesNotContainKey("preboot-custom-attributes");
    }

    @Test
    void shouldHandleNullCustomAttributes() {
        FileMetadata metadata =
                new FileMetadata(FILE_ID, "test.pdf", "application/pdf", 1024L, AUTHOR_ID, TENANT_ID, NOW, NOW, null);

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        PutObjectRequest.Builder result = S3MetadataMapper.addMetadata(builder, metadata);

        PutObjectRequest request = result.build();
        Map<String, String> s3Metadata = request.metadata();

        assertThat(s3Metadata).doesNotContainKey("preboot-custom-attributes");
    }

    @Test
    void shouldCreateMetadataWithS3Info() {
        GetObjectResponse response = GetObjectResponse.builder()
                .contentLength(1024L)
                .lastModified(NOW)
                .build();

        FileMetadata metadata = S3MetadataMapper.createMetadataWithS3Info(
                response, S3_KEY, "test.pdf", "application/pdf", AUTHOR_ID, TENANT_ID);

        assertThat(metadata.fileId()).isEqualTo(FILE_ID);
        assertThat(metadata.fileName()).isEqualTo("test.pdf");
        assertThat(metadata.contentType()).isEqualTo("application/pdf");
        assertThat(metadata.fileSize()).isEqualTo(1024L);
        assertThat(metadata.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(metadata.tenantId()).isEqualTo(TENANT_ID);
        assertThat(metadata.createdAt()).isEqualTo(NOW);
        assertThat(metadata.lastModified()).isEqualTo(NOW);
        assertThat(metadata.customAttributes()).isEmpty();
    }

    @Test
    void shouldRoundTripMetadata() {
        Map<String, String> customAttributes = Map.of("category", "document", "priority", "high");

        FileMetadata originalMetadata = new FileMetadata(
                FILE_ID, "test.pdf", "application/pdf", 1024L, AUTHOR_ID, TENANT_ID, NOW, NOW, customAttributes);

        // Convert to S3 metadata
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        PutObjectRequest request =
                S3MetadataMapper.addMetadata(builder, originalMetadata).build();

        // Simulate getting back from S3
        GetObjectResponse response =
                GetObjectResponse.builder().metadata(request.metadata()).build();

        FileMetadata extractedMetadata = S3MetadataMapper.extractMetadata(response, S3_KEY);

        assertThat(extractedMetadata.fileId()).isEqualTo(originalMetadata.fileId());
        assertThat(extractedMetadata.fileName()).isEqualTo(originalMetadata.fileName());
        assertThat(extractedMetadata.contentType()).isEqualTo(originalMetadata.contentType());
        assertThat(extractedMetadata.fileSize()).isEqualTo(originalMetadata.fileSize());
        assertThat(extractedMetadata.authorId()).isEqualTo(originalMetadata.authorId());
        assertThat(extractedMetadata.tenantId()).isEqualTo(originalMetadata.tenantId());
        assertThat(extractedMetadata.createdAt()).isEqualTo(originalMetadata.createdAt());
        assertThat(extractedMetadata.lastModified()).isEqualTo(originalMetadata.lastModified());
        assertThat(extractedMetadata.customAttributes()).isEqualTo(originalMetadata.customAttributes());
    }
}
