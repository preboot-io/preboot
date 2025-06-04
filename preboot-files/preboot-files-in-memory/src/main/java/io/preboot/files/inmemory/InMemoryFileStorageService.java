package io.preboot.files.inmemory;

import io.preboot.eventbus.EventPublisher;
import io.preboot.files.api.FileFilter;
import io.preboot.files.api.FileStorageException;
import io.preboot.files.api.FileStorageService;
import io.preboot.files.events.FileAccessedEvent;
import io.preboot.files.events.FileDeletedEvent;
import io.preboot.files.events.FileStoredEvent;
import io.preboot.files.model.FileContent;
import io.preboot.files.model.FileMetadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InMemoryFileStorageService implements FileStorageService {

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, StoredFile>> tenantStorage;

    private final ConcurrentHashMap<UUID, AtomicLong> tenantStorageUsage;

    private final EventPublisher eventPublisher;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private record StoredFile(FileMetadata metadata, byte[] content) {
        public long getMemoryFootprint() {
            return content.length + estimateMetadataSize();
        }

        private long estimateMetadataSize() {
            long uuidSize = 36 * 2;
            return metadata.fileName().length() * 2L
                    + metadata.contentType().length() * 2L
                    + uuidSize
                    + uuidSize
                    + 200; // Object overhead + timestamps + primitives
        }
    }

    public InMemoryFileStorageService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.tenantStorage = new ConcurrentHashMap<>();
        this.tenantStorageUsage = new ConcurrentHashMap<>();

        log.info("InMemoryFileStorageService initialized with concurrent storage architecture");
    }

    @Override
    public CompletableFuture<FileMetadata> storeFile(
            String fileName, String contentType, InputStream content, UUID authorId, UUID tenantId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug(
                        "Starting file storage operation - fileName: {}, tenantId: {}, authorId: {}",
                        fileName,
                        tenantId,
                        authorId);

                validateStoreParameters(fileName, contentType, authorId, tenantId);

                byte[] fileBytes = readAndValidateContent(content, fileName);

                FileMetadata metadata =
                        FileMetadata.create(fileName, contentType, fileBytes.length, authorId, tenantId);

                StoredFile storedFile = new StoredFile(metadata, fileBytes);
                storeFileAtomically(tenantId, metadata.fileId(), storedFile);

                updateStorageUsage(tenantId, fileBytes.length);

                publishStorageEvent(metadata);

                log.info(
                        "File stored successfully - fileId: {}, size: {} bytes, tenant: {}",
                        metadata.fileId(),
                        fileBytes.length,
                        tenantId);

                return metadata;

            } catch (IOException e) {
                log.error("IO error during file storage - fileName: {}, tenant: {}", fileName, tenantId, e);
                throw new FileStorageException("Failed to read file content: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during file storage - fileName: {}, tenant: {}", fileName, tenantId, e);
                throw new FileStorageException("File storage operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<FileContent>> retrieveFile(UUID fileId, UUID tenantId) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Starting file retrieval operation - fileId: {}, tenantId: {}", fileId, tenantId);

            ConcurrentHashMap<UUID, StoredFile> tenantFiles = tenantStorage.get(tenantId);
            if (tenantFiles == null) {
                log.debug("No storage found for tenant: {}", tenantId);
                return Optional.empty();
            }

            StoredFile storedFile = tenantFiles.get(fileId);
            if (storedFile == null) {
                log.debug("File not found - fileId: {}, tenantId: {}", fileId, tenantId);
                return Optional.empty();
            }

            ByteArrayInputStream contentStream = new ByteArrayInputStream(storedFile.content());
            FileContent fileContent = new FileContent(storedFile.metadata(), contentStream);

            publishAccessEvent(fileId, tenantId, storedFile.metadata().authorId());

            log.debug("File retrieved successfully - fileId: {}, size: {} bytes", fileId, storedFile.content().length);

            return Optional.of(fileContent);
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteFile(UUID fileId, UUID tenantId) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Starting file deletion operation - fileId: {}, tenantId: {}", fileId, tenantId);

            StoredFile removedFile = removeFileAtomically(tenantId, fileId);
            if (removedFile == null) {
                log.debug("File not found for deletion - fileId: {}, tenantId: {}", fileId, tenantId);
                return false;
            }

            updateStorageUsage(tenantId, -removedFile.content().length);

            publishDeletionEvent(fileId, tenantId, removedFile.metadata().authorId());

            log.info("File deleted successfully - fileId: {}, tenantId: {}", fileId, tenantId);
            return true;
        });
    }

    @Override
    public CompletableFuture<Optional<FileMetadata>> getFileMetadata(UUID fileId, UUID tenantId) {

        return CompletableFuture.supplyAsync(() -> {
            return Optional.ofNullable(tenantStorage.get(tenantId))
                    .map(tenantFiles -> tenantFiles.get(fileId))
                    .map(StoredFile::metadata);
        });
    }

    @Override
    public CompletableFuture<List<FileMetadata>> listFiles(UUID tenantId, FileFilter filter) {

        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<UUID, StoredFile> tenantFiles = tenantStorage.get(tenantId);
            if (tenantFiles == null) {
                return Collections.emptyList();
            }

            return tenantFiles.values().parallelStream()
                    .map(StoredFile::metadata)
                    .filter(filter::matches)
                    .toList();
        });
    }

    @Override
    public CompletableFuture<Long> getTotalStorageUsed(UUID tenantId) {

        return CompletableFuture.supplyAsync(() -> {
            AtomicLong usage = tenantStorageUsage.get(tenantId);
            return usage != null ? usage.get() : 0L;
        });
    }

    @Override
    public CompletableFuture<List<FileMetadata>> getFilesByAuthor(UUID authorId, UUID tenantId) {

        return CompletableFuture.supplyAsync(() -> {
            ConcurrentHashMap<UUID, StoredFile> tenantFiles = tenantStorage.get(tenantId);
            if (tenantFiles == null) {
                return Collections.emptyList();
            }

            return tenantFiles.values().parallelStream()
                    .map(StoredFile::metadata)
                    .filter(metadata -> Objects.equals(metadata.authorId(), authorId))
                    .toList();
        });
    }

    private void validateStoreParameters(String fileName, String contentType, UUID authorId, UUID tenantId) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
    }

    private byte[] readAndValidateContent(InputStream content, String fileName) throws IOException {

        if (content == null) {
            throw new IllegalArgumentException("Content stream cannot be null");
        }

        byte[] fileBytes = content.readAllBytes();

        if (fileBytes.length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new FileStorageException(String.format(
                    "File size (%d bytes) exceeds maximum limit (%d bytes) for file: %s",
                    fileBytes.length, MAX_FILE_SIZE, fileName));
        }

        return fileBytes;
    }

    private void storeFileAtomically(UUID tenantId, UUID fileId, StoredFile storedFile) {
        tenantStorage.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>()).put(fileId, storedFile);
    }

    private StoredFile removeFileAtomically(UUID tenantId, UUID fileId) {
        ConcurrentHashMap<UUID, StoredFile> tenantFiles = tenantStorage.get(tenantId);
        if (tenantFiles == null) {
            return null;
        }

        return tenantFiles.remove(fileId);
    }

    private void updateStorageUsage(UUID tenantId, long sizeChange) {
        tenantStorageUsage.computeIfAbsent(tenantId, k -> new AtomicLong(0)).addAndGet(sizeChange);
    }

    private void publishStorageEvent(FileMetadata metadata) {
        try {
            eventPublisher.publish(new FileStoredEvent(metadata));
        } catch (Exception e) {
            log.warn("Failed to publish file storage event for fileId: {}", metadata.fileId(), e);
        }
    }

    private void publishAccessEvent(UUID fileId, UUID tenantId, UUID originalAuthorId) {
        try {
            eventPublisher.publish(new FileAccessedEvent(fileId, tenantId, originalAuthorId, Instant.now()));
        } catch (Exception e) {
            log.warn("Failed to publish file access event for fileId: {}", fileId, e);
        }
    }

    private void publishDeletionEvent(UUID fileId, UUID tenantId, UUID authorId) {
        try {
            eventPublisher.publish(new FileDeletedEvent(fileId, tenantId, authorId));
        } catch (Exception e) {
            log.warn("Failed to publish file deletion event for fileId: {}", fileId, e);
        }
    }
}
