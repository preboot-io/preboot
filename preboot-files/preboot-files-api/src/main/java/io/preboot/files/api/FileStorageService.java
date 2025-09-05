package io.preboot.files.api;

import io.preboot.files.model.FileContent;
import io.preboot.files.model.FileMetadata;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FileStorageService {

    CompletableFuture<FileMetadata> storeFile(
            String fileName, String contentType, InputStream content, UUID authorId, UUID tenantId);

    CompletableFuture<FileMetadata> storeFile(
            String fileName,
            String contentType,
            InputStream content,
            UUID authorId,
            UUID tenantId,
            Map<String, String> customAttributes);

    CompletableFuture<Optional<FileContent>> retrieveFile(UUID fileId, UUID tenantId);

    CompletableFuture<Boolean> deleteFile(UUID fileId, UUID tenantId);

    CompletableFuture<Optional<FileMetadata>> getFileMetadata(UUID fileId, UUID tenantId);

    CompletableFuture<List<FileMetadata>> listFiles(UUID tenantId, FileFilter filter);

    CompletableFuture<Long> getTotalStorageUsed(UUID tenantId);

    CompletableFuture<List<FileMetadata>> getFilesByAuthor(UUID authorId, UUID tenantId);
}
