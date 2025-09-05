package io.preboot.files.s3;

import static org.assertj.core.api.Assertions.*;

import io.preboot.eventbus.EventPublisher;
import io.preboot.files.model.FileContent;
import io.preboot.files.model.FileMetadata;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Testcontainers
class S3FileStorageServiceIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.S3);

    private S3FileStorageService service;
    private S3Client s3Client;
    private EventPublisher eventPublisher;

    private static final String BUCKET_NAME = "test-integration-bucket";
    private static final UUID TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String FILE_NAME = "integration-test.pdf";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final byte[] FILE_CONTENT = "Integration test content".getBytes();

    @BeforeEach
    void setUp() {
        // Create S3 client pointing to LocalStack
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .build();

        // Create test bucket
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

        // Create service with LocalStack configuration
        S3FileStorageProperties properties = new S3FileStorageProperties(
                BUCKET_NAME,
                localstack.getRegion(),
                localstack.getEndpoint().toString(),
                localstack.getAccessKey(),
                localstack.getSecretKey(),
                52428800L,
                false);

        eventPublisher = Mockito.mock(EventPublisher.class);
        service = new S3FileStorageService(s3Client, eventPublisher, properties);
    }

    @Test
    void shouldPerformFullFileLifecycleWithRealS3() {
        InputStream content = new ByteArrayInputStream(FILE_CONTENT);

        // Store file
        FileMetadata storedMetadata = service.storeFile(FILE_NAME, CONTENT_TYPE, content, AUTHOR_ID, TENANT_ID)
                .join();

        assertThat(storedMetadata).isNotNull();
        assertThat(storedMetadata.fileName()).isEqualTo(FILE_NAME);
        assertThat(storedMetadata.contentType()).isEqualTo(CONTENT_TYPE);
        assertThat(storedMetadata.fileSize()).isEqualTo(FILE_CONTENT.length);
        assertThat(storedMetadata.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(storedMetadata.tenantId()).isEqualTo(TENANT_ID);

        // Retrieve file metadata
        Optional<FileMetadata> retrievedMetadata =
                service.getFileMetadata(storedMetadata.fileId(), TENANT_ID).join();

        assertThat(retrievedMetadata).isPresent();
        assertThat(retrievedMetadata.get().fileId()).isEqualTo(storedMetadata.fileId());
        assertThat(retrievedMetadata.get().fileName()).isEqualTo(FILE_NAME);

        // Retrieve file content
        Optional<FileContent> retrievedContent =
                service.retrieveFile(storedMetadata.fileId(), TENANT_ID).join();

        assertThat(retrievedContent).isPresent();
        assertThat(retrievedContent.get().metadata().fileId()).isEqualTo(storedMetadata.fileId());

        // List files
        List<FileMetadata> fileList = service.listFiles(TENANT_ID, null).join();

        assertThat(fileList).hasSize(1);
        assertThat(fileList.get(0).fileId()).isEqualTo(storedMetadata.fileId());

        // Check storage usage
        Long totalStorage = service.getTotalStorageUsed(TENANT_ID).join();

        assertThat(totalStorage).isEqualTo(FILE_CONTENT.length);

        // Get files by author
        List<FileMetadata> authorFiles =
                service.getFilesByAuthor(AUTHOR_ID, TENANT_ID).join();

        assertThat(authorFiles).hasSize(1);
        assertThat(authorFiles.get(0).fileId()).isEqualTo(storedMetadata.fileId());

        // Delete file
        Boolean deleted = service.deleteFile(storedMetadata.fileId(), TENANT_ID).join();

        assertThat(deleted).isTrue();

        // Verify file is gone
        Optional<FileMetadata> deletedMetadata =
                service.getFileMetadata(storedMetadata.fileId(), TENANT_ID).join();

        assertThat(deletedMetadata).isEmpty();

        // Verify storage usage is now zero
        Long finalStorage = service.getTotalStorageUsed(TENANT_ID).join();

        assertThat(finalStorage).isZero();
    }

    @Test
    void shouldHandleMultipleFilesForSameTenant() {
        String fileName1 = "file1.txt";
        String fileName2 = "file2.txt";
        byte[] content1 = "Content 1".getBytes();
        byte[] content2 = "Content 2".getBytes();

        // Store two files
        FileMetadata metadata1 = service.storeFile(
                        fileName1, "text/plain", new ByteArrayInputStream(content1), AUTHOR_ID, TENANT_ID)
                .join();
        FileMetadata metadata2 = service.storeFile(
                        fileName2, "text/plain", new ByteArrayInputStream(content2), AUTHOR_ID, TENANT_ID)
                .join();

        // List files should return both
        List<FileMetadata> fileList = service.listFiles(TENANT_ID, null).join();

        assertThat(fileList).hasSize(2);
        assertThat(fileList).extracting(FileMetadata::fileName).containsExactlyInAnyOrder(fileName1, fileName2);

        // Total storage should be sum of both files
        Long totalStorage = service.getTotalStorageUsed(TENANT_ID).join();

        assertThat(totalStorage).isEqualTo(content1.length + content2.length);

        // Clean up
        service.deleteFile(metadata1.fileId(), TENANT_ID).join();
        service.deleteFile(metadata2.fileId(), TENANT_ID).join();
    }

    @Test
    void shouldIsolateTenants() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        // Store file for tenant1
        FileMetadata metadata1 = service.storeFile(
                        "tenant1-file.txt",
                        "text/plain",
                        new ByteArrayInputStream("Tenant 1 content".getBytes()),
                        AUTHOR_ID,
                        tenant1)
                .join();

        // Store file for tenant2
        FileMetadata metadata2 = service.storeFile(
                        "tenant2-file.txt",
                        "text/plain",
                        new ByteArrayInputStream("Tenant 2 content".getBytes()),
                        AUTHOR_ID,
                        tenant2)
                .join();

        // Each tenant should only see their own files
        List<FileMetadata> tenant1Files = service.listFiles(tenant1, null).join();
        List<FileMetadata> tenant2Files = service.listFiles(tenant2, null).join();

        assertThat(tenant1Files).hasSize(1);
        assertThat(tenant1Files.get(0).fileId()).isEqualTo(metadata1.fileId());

        assertThat(tenant2Files).hasSize(1);
        assertThat(tenant2Files.get(0).fileId()).isEqualTo(metadata2.fileId());

        // Cross-tenant access should fail
        Optional<FileContent> crossAccess =
                service.retrieveFile(metadata1.fileId(), tenant2).join();

        assertThat(crossAccess).isEmpty();

        // Clean up
        service.deleteFile(metadata1.fileId(), tenant1).join();
        service.deleteFile(metadata2.fileId(), tenant2).join();
    }
}
