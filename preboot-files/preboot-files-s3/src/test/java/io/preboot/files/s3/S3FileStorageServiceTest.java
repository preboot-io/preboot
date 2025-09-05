package io.preboot.files.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.preboot.eventbus.EventPublisher;
import io.preboot.files.api.FileStorageException;
import io.preboot.files.events.FileAccessedEvent;
import io.preboot.files.events.FileDeletedEvent;
import io.preboot.files.events.FileStoredEvent;
import io.preboot.files.model.FileContent;
import io.preboot.files.model.FileMetadata;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private EventPublisher eventPublisher;

    private S3FileStorageService service;
    private S3FileStorageProperties properties;

    private static final UUID TENANT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID FILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String BUCKET_NAME = "test-bucket";
    private static final String FILE_NAME = "test.pdf";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final byte[] FILE_CONTENT = "test content".getBytes();

    @BeforeEach
    void setUp() {
        properties = new S3FileStorageProperties(BUCKET_NAME, "us-east-1", null, null, null, 52428800L, false);
        service = new S3FileStorageService(s3Client, eventPublisher, properties);
    }

    @Test
    void shouldStoreFileSuccessfully() {
        // Given
        InputStream content = new ByteArrayInputStream(FILE_CONTENT);
        PutObjectResponse putResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putResponse);

        // When
        CompletableFuture<FileMetadata> result =
                service.storeFile(FILE_NAME, CONTENT_TYPE, content, AUTHOR_ID, TENANT_ID);
        FileMetadata metadata = result.join();

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.fileName()).isEqualTo(FILE_NAME);
        assertThat(metadata.contentType()).isEqualTo(CONTENT_TYPE);
        assertThat(metadata.fileSize()).isEqualTo(FILE_CONTENT.length);
        assertThat(metadata.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(metadata.tenantId()).isEqualTo(TENANT_ID);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(eventPublisher).publish(any(FileStoredEvent.class));
    }

    @Test
    void shouldRetrieveFileSuccessfully() {
        // Given
        Map<String, String> s3Metadata = createTestS3Metadata();

        GetObjectResponse getResponse =
                GetObjectResponse.builder().metadata(s3Metadata).build();

        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);
        when(responseStream.response()).thenReturn(getResponse);

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        // When
        CompletableFuture<Optional<FileContent>> result = service.retrieveFile(FILE_ID, TENANT_ID);
        Optional<FileContent> fileContent = result.join();

        // Then
        assertThat(fileContent).isPresent();
        assertThat(fileContent.get().metadata().fileId()).isEqualTo(FILE_ID);
        assertThat(fileContent.get().metadata().fileName()).isEqualTo(FILE_NAME);

        verify(s3Client).getObject(any(GetObjectRequest.class));
        verify(eventPublisher).publish(any(FileAccessedEvent.class));
    }

    @Test
    void shouldReturnEmptyWhenFileNotFound() {
        // Given
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // When
        CompletableFuture<Optional<FileContent>> result = service.retrieveFile(FILE_ID, TENANT_ID);
        Optional<FileContent> fileContent = result.join();

        // Then
        assertThat(fileContent).isEmpty();
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldDeleteFileSuccessfully() {
        // Given
        Map<String, String> s3Metadata = createTestS3Metadata();
        HeadObjectResponse headResponse =
                HeadObjectResponse.builder().metadata(s3Metadata).build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        // When
        CompletableFuture<Boolean> result = service.deleteFile(FILE_ID, TENANT_ID);
        Boolean deleted = result.join();

        // Then
        assertThat(deleted).isTrue();
        verify(s3Client).headObject(any(HeadObjectRequest.class));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        verify(eventPublisher).publish(any(FileDeletedEvent.class));
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentFile() {
        // Given
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // When
        CompletableFuture<Boolean> result = service.deleteFile(FILE_ID, TENANT_ID);
        Boolean deleted = result.join();

        // Then
        assertThat(deleted).isFalse();
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldGetFileMetadataSuccessfully() {
        // Given
        Map<String, String> s3Metadata = createTestS3Metadata();
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .metadata(s3Metadata)
                .contentLength((long) FILE_CONTENT.length)
                .lastModified(Instant.now())
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        // When
        CompletableFuture<Optional<FileMetadata>> result = service.getFileMetadata(FILE_ID, TENANT_ID);
        Optional<FileMetadata> metadata = result.join();

        // Then
        assertThat(metadata).isPresent();
        assertThat(metadata.get().fileId()).isEqualTo(FILE_ID);
        assertThat(metadata.get().fileName()).isEqualTo(FILE_NAME);
        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void shouldReturnEmptyMetadataWhenFileNotFound() {
        // Given
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        // When
        CompletableFuture<Optional<FileMetadata>> result = service.getFileMetadata(FILE_ID, TENANT_ID);
        Optional<FileMetadata> metadata = result.join();

        // Then
        assertThat(metadata).isEmpty();
    }

    @Test
    void shouldListFilesSuccessfully() {
        // Given
        String s3Key = S3KeyUtil.generateKey(TENANT_ID, FILE_ID);
        S3Object s3Object =
                S3Object.builder().key(s3Key).size((long) FILE_CONTENT.length).build();

        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(s3Object)
                .isTruncated(false)
                .build();

        Map<String, String> s3Metadata = createTestS3Metadata();
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .metadata(s3Metadata)
                .contentLength((long) FILE_CONTENT.length)
                .lastModified(Instant.now())
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        // When
        CompletableFuture<List<FileMetadata>> result = service.listFiles(TENANT_ID, null);
        List<FileMetadata> files = result.join();

        // Then
        assertThat(files).hasSize(1);
        assertThat(files.get(0).fileId()).isEqualTo(FILE_ID);
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void shouldCalculateTotalStorageUsed() {
        // Given
        S3Object s3Object = S3Object.builder()
                .key(S3KeyUtil.generateKey(TENANT_ID, FILE_ID))
                .size(1024L)
                .build();

        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(s3Object)
                .isTruncated(false)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

        // When
        CompletableFuture<Long> result = service.getTotalStorageUsed(TENANT_ID);
        Long totalSize = result.join();

        // Then
        assertThat(totalSize).isEqualTo(1024L);
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void shouldThrowExceptionWhenFileTooLarge() {
        // Given
        byte[] largeContent = new byte[(int) properties.maxFileSize() + 1];
        InputStream content = new ByteArrayInputStream(largeContent);

        // When/Then
        assertThatThrownBy(() -> {
                    CompletableFuture<FileMetadata> result =
                            service.storeFile(FILE_NAME, CONTENT_TYPE, content, AUTHOR_ID, TENANT_ID);
                    result.join();
                })
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(FileStorageException.class)
                .hasRootCauseMessage(
                        "File size (52428801 bytes) exceeds maximum allowed size (52428800 bytes) for file: test.pdf");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldThrowExceptionWhenFileNameIsNull() {
        // Given
        InputStream content = new ByteArrayInputStream(FILE_CONTENT);

        // When/Then
        assertThatThrownBy(() -> {
                    CompletableFuture<FileMetadata> result =
                            service.storeFile(null, CONTENT_TYPE, content, AUTHOR_ID, TENANT_ID);
                    result.join();
                })
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(FileStorageException.class);
    }

    @Test
    void shouldHandleS3ExceptionDuringStore() {
        // Given
        InputStream content = new ByteArrayInputStream(FILE_CONTENT);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        // When/Then
        assertThatThrownBy(() -> {
                    CompletableFuture<FileMetadata> result =
                            service.storeFile(FILE_NAME, CONTENT_TYPE, content, AUTHOR_ID, TENANT_ID);
                    result.join();
                })
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(FileStorageException.class);
    }

    private Map<String, String> createTestS3Metadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("preboot-file-id", FILE_ID.toString());
        metadata.put("preboot-file-name", FILE_NAME);
        metadata.put("preboot-content-type", CONTENT_TYPE);
        metadata.put("preboot-file-size", String.valueOf(FILE_CONTENT.length));
        metadata.put("preboot-author-id", AUTHOR_ID.toString());
        metadata.put("preboot-tenant-id", TENANT_ID.toString());
        metadata.put("preboot-created-at", Instant.now().toString());
        metadata.put("preboot-last-modified", Instant.now().toString());
        return metadata;
    }
}
