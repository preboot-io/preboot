package io.preboot.files.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.preboot.eventbus.EventPublisher;
import io.preboot.files.api.FileFilter;
import io.preboot.files.api.FileStorageException;
import io.preboot.files.events.FileDeletedEvent;
import io.preboot.files.events.FileStoredEvent;
import io.preboot.files.model.FileContent;
import io.preboot.files.model.FileMetadata;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryFileStorageServiceTest {

    private InMemoryFileStorageService fileStorageService;
    private EventPublisher eventPublisher;

    private static final String TEST_FILE_NAME = "test.txt";
    private static final String TEST_CONTENT_TYPE = "text/plain";
    private static final byte[] TEST_CONTENT = "Test file content".getBytes();
    private static final UUID TEST_AUTHOR_ID = UUID.randomUUID();
    private static final UUID TEST_TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        eventPublisher = mock(EventPublisher.class);
        fileStorageService = new InMemoryFileStorageService(eventPublisher);
    }

    @Test
    void whenStoreFile_thenFileStoredSuccessfully() throws Exception {
        // Given
        InputStream contentStream = new ByteArrayInputStream(TEST_CONTENT);

        // When
        CompletableFuture<FileMetadata> result = fileStorageService.storeFile(
                TEST_FILE_NAME, TEST_CONTENT_TYPE, contentStream, TEST_AUTHOR_ID, TEST_TENANT_ID);

        // Then
        FileMetadata metadata = result.get();
        assertThat(metadata.fileName()).isEqualTo(TEST_FILE_NAME);
        assertThat(metadata.contentType()).isEqualTo(TEST_CONTENT_TYPE);
        assertThat(metadata.fileSize()).isEqualTo(TEST_CONTENT.length);
        assertThat(metadata.authorId()).isEqualTo(TEST_AUTHOR_ID);
        assertThat(metadata.tenantId()).isEqualTo(TEST_TENANT_ID);
        assertThat(metadata.fileId()).isNotNull();

        verify(eventPublisher).publish(any(FileStoredEvent.class));
    }

    @Test
    void whenStoreFileWithNullFileName_thenThrowException() {
        // Given
        InputStream contentStream = new ByteArrayInputStream(TEST_CONTENT);

        // When & Then
        assertThatThrownBy(() -> fileStorageService
                        .storeFile(null, TEST_CONTENT_TYPE, contentStream, TEST_AUTHOR_ID, TEST_TENANT_ID)
                        .get())
                .hasCauseInstanceOf(FileStorageException.class)
                .hasMessageContaining("File name cannot be null or empty");
    }

    @Test
    void whenStoreFileWithEmptyContent_thenThrowException() {
        // Given
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        // When & Then
        assertThatThrownBy(() -> fileStorageService
                        .storeFile(TEST_FILE_NAME, TEST_CONTENT_TYPE, emptyStream, TEST_AUTHOR_ID, TEST_TENANT_ID)
                        .get())
                .hasCauseInstanceOf(FileStorageException.class)
                .hasMessageContaining("File content cannot be empty");
    }

    @Test
    void whenStoreFileTooLarge_thenThrowException() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB - over the 10MB limit
        InputStream largeStream = new ByteArrayInputStream(largeContent);

        // When & Then
        assertThatThrownBy(() -> fileStorageService
                        .storeFile(TEST_FILE_NAME, TEST_CONTENT_TYPE, largeStream, TEST_AUTHOR_ID, TEST_TENANT_ID)
                        .get())
                .hasCauseInstanceOf(FileStorageException.class)
                .hasMessageContaining("exceeds maximum limit");
    }

    @Test
    void whenRetrieveExistingFile_thenFileReturned() throws Exception {
        // Given
        InputStream contentStream = new ByteArrayInputStream(TEST_CONTENT);
        FileMetadata storedMetadata = fileStorageService
                .storeFile(TEST_FILE_NAME, TEST_CONTENT_TYPE, contentStream, TEST_AUTHOR_ID, TEST_TENANT_ID)
                .get();

        // When
        CompletableFuture<Optional<FileContent>> result =
                fileStorageService.retrieveFile(storedMetadata.fileId(), TEST_TENANT_ID);

        // Then
        Optional<FileContent> fileContent = result.get();
        assertThat(fileContent).isPresent();

        try (FileContent content = fileContent.get()) {
            assertThat(content.metadata().fileId()).isEqualTo(storedMetadata.fileId());
            assertThat(content.toByteArray()).isEqualTo(TEST_CONTENT);
        }
    }

    @Test
    void whenRetrieveNonExistentFile_thenEmptyReturned() throws Exception {
        // Given
        UUID nonExistentFileId = UUID.randomUUID();

        // When
        CompletableFuture<Optional<FileContent>> result =
                fileStorageService.retrieveFile(nonExistentFileId, TEST_TENANT_ID);

        // Then
        Optional<FileContent> fileContent = result.get();
        assertThat(fileContent).isEmpty();
    }

    @Test
    void whenDeleteExistingFile_thenFileDeletedSuccessfully() throws Exception {
        // Given
        InputStream contentStream = new ByteArrayInputStream(TEST_CONTENT);
        FileMetadata storedMetadata = fileStorageService
                .storeFile(TEST_FILE_NAME, TEST_CONTENT_TYPE, contentStream, TEST_AUTHOR_ID, TEST_TENANT_ID)
                .get();

        // When
        CompletableFuture<Boolean> result = fileStorageService.deleteFile(storedMetadata.fileId(), TEST_TENANT_ID);

        // Then
        Boolean deleted = result.get();
        assertThat(deleted).isTrue();

        // Verify file is gone
        Optional<FileContent> retrievedFile = fileStorageService
                .retrieveFile(storedMetadata.fileId(), TEST_TENANT_ID)
                .get();
        assertThat(retrievedFile).isEmpty();

        verify(eventPublisher).publish(any(FileDeletedEvent.class));
    }

    @Test
    void whenDeleteNonExistentFile_thenReturnFalse() throws Exception {
        // Given
        UUID nonExistentFileId = UUID.randomUUID();

        // When
        CompletableFuture<Boolean> result = fileStorageService.deleteFile(nonExistentFileId, TEST_TENANT_ID);

        // Then
        Boolean deleted = result.get();
        assertThat(deleted).isFalse();
    }

    @Test
    void whenGetFileMetadata_thenMetadataReturned() throws Exception {
        // Given
        InputStream contentStream = new ByteArrayInputStream(TEST_CONTENT);
        FileMetadata storedMetadata = fileStorageService
                .storeFile(TEST_FILE_NAME, TEST_CONTENT_TYPE, contentStream, TEST_AUTHOR_ID, TEST_TENANT_ID)
                .get();

        // When
        CompletableFuture<Optional<FileMetadata>> result =
                fileStorageService.getFileMetadata(storedMetadata.fileId(), TEST_TENANT_ID);

        // Then
        Optional<FileMetadata> metadata = result.get();
        assertThat(metadata).isPresent();
        assertThat(metadata.get().fileId()).isEqualTo(storedMetadata.fileId());
        assertThat(metadata.get().fileName()).isEqualTo(TEST_FILE_NAME);
    }

    @Test
    void whenListFilesWithFilter_thenFilteredFilesReturned() throws Exception {
        // Given
        InputStream contentStream1 = new ByteArrayInputStream(TEST_CONTENT);
        InputStream contentStream2 = new ByteArrayInputStream("Different content".getBytes());

        FileMetadata metadata1 = fileStorageService
                .storeFile("file1.txt", "text/plain", contentStream1, TEST_AUTHOR_ID, TEST_TENANT_ID)
                .get();
        fileStorageService
                .storeFile("file2.pdf", "application/pdf", contentStream2, UUID.randomUUID(), TEST_TENANT_ID)
                .get();

        // When
        FileFilter authorFilter = FileFilter.byAuthor(TEST_AUTHOR_ID);
        CompletableFuture<List<FileMetadata>> result = fileStorageService.listFiles(TEST_TENANT_ID, authorFilter);

        // Then
        List<FileMetadata> files = result.get();
        assertThat(files).hasSize(1);
        assertThat(files.get(0).fileId()).isEqualTo(metadata1.fileId());
    }

    @Test
    void whenGetTotalStorageUsed_thenCorrectSizeReturned() throws Exception {
        // Given
        InputStream contentStream1 = new ByteArrayInputStream(TEST_CONTENT);
        InputStream contentStream2 = new ByteArrayInputStream("Different content".getBytes());

        fileStorageService
                .storeFile("file1.txt", "text/plain", contentStream1, TEST_AUTHOR_ID, TEST_TENANT_ID)
                .get();
        fileStorageService
                .storeFile("file2.txt", "text/plain", contentStream2, TEST_AUTHOR_ID, TEST_TENANT_ID)
                .get();

        // When
        CompletableFuture<Long> result = fileStorageService.getTotalStorageUsed(TEST_TENANT_ID);

        // Then
        Long totalSize = result.get();
        long expectedSize = TEST_CONTENT.length + "Different content".getBytes().length;
        assertThat(totalSize).isEqualTo(expectedSize);
    }

    @Test
    void whenGetFilesByAuthor_thenAuthorFilesReturned() throws Exception {
        // Given
        UUID otherAuthorId = UUID.randomUUID();
        InputStream contentStream1 = new ByteArrayInputStream(TEST_CONTENT);
        InputStream contentStream2 = new ByteArrayInputStream("Different content".getBytes());

        FileMetadata metadata1 = fileStorageService
                .storeFile("file1.txt", "text/plain", contentStream1, TEST_AUTHOR_ID, TEST_TENANT_ID)
                .get();
        fileStorageService
                .storeFile("file2.txt", "text/plain", contentStream2, otherAuthorId, TEST_TENANT_ID)
                .get();

        // When
        CompletableFuture<List<FileMetadata>> result =
                fileStorageService.getFilesByAuthor(TEST_AUTHOR_ID, TEST_TENANT_ID);

        // Then
        List<FileMetadata> files = result.get();
        assertThat(files).hasSize(1);
        assertThat(files.get(0).fileId()).isEqualTo(metadata1.fileId());
        assertThat(files.get(0).authorId()).isEqualTo(TEST_AUTHOR_ID);
    }

    @Test
    void whenMultipleTenantsStoreFiles_thenTenantIsolationMaintained() throws Exception {
        // Given
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        InputStream contentStream1 = new ByteArrayInputStream(TEST_CONTENT);
        InputStream contentStream2 = new ByteArrayInputStream("Tenant 2 content".getBytes());

        FileMetadata metadata1 = fileStorageService
                .storeFile("file1.txt", "text/plain", contentStream1, TEST_AUTHOR_ID, tenant1)
                .get();
        FileMetadata metadata2 = fileStorageService
                .storeFile("file2.txt", "text/plain", contentStream2, TEST_AUTHOR_ID, tenant2)
                .get();

        // When & Then
        // Tenant 1 should only see its file
        Optional<FileContent> tenant1File =
                fileStorageService.retrieveFile(metadata1.fileId(), tenant1).get();
        assertThat(tenant1File).isPresent();

        Optional<FileContent> crossTenantAccess =
                fileStorageService.retrieveFile(metadata2.fileId(), tenant1).get();
        assertThat(crossTenantAccess).isEmpty();

        // Tenant 2 should only see its file
        Optional<FileContent> tenant2File =
                fileStorageService.retrieveFile(metadata2.fileId(), tenant2).get();
        assertThat(tenant2File).isPresent();
    }
}
