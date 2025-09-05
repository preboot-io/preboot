package io.preboot.files.s3;

import io.preboot.eventbus.EventPublisher;
import io.preboot.files.api.FileFilter;
import io.preboot.files.api.FileStorageException;
import io.preboot.files.api.FileStorageService;
import io.preboot.files.events.FileAccessedEvent;
import io.preboot.files.events.FileDeletedEvent;
import io.preboot.files.events.FileStoredEvent;
import io.preboot.files.model.FileContent;
import io.preboot.files.model.FileMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Slf4j
@Service
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final EventPublisher eventPublisher;
    private final S3FileStorageProperties properties;

    public S3FileStorageService(S3Client s3Client, EventPublisher eventPublisher, S3FileStorageProperties properties) {
        this.s3Client = s3Client;
        this.eventPublisher = eventPublisher;
        this.properties = properties;

        log.info(
                "S3FileStorageService initialized with bucket: {}, max file size: {} bytes ({}MB)",
                properties.bucketName(),
                properties.maxFileSize(),
                properties.maxFileSize() / (1024 * 1024));
    }

    @Override
    public CompletableFuture<FileMetadata> storeFile(
            String fileName, String contentType, InputStream content, UUID authorId, UUID tenantId) {
        return storeFile(fileName, contentType, content, authorId, tenantId, null);
    }

    @Override
    public CompletableFuture<FileMetadata> storeFile(
            String fileName,
            String contentType,
            InputStream content,
            UUID authorId,
            UUID tenantId,
            Map<String, String> customAttributes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug(
                        "Starting S3 file storage operation - fileName: {}, tenantId: {}, authorId: {}",
                        fileName,
                        tenantId,
                        authorId);

                validateStoreParameters(fileName, contentType, authorId, tenantId);
                byte[] fileBytes = readAndValidateContent(content, fileName);

                FileMetadata metadata =
                        FileMetadata.create(fileName, contentType, fileBytes.length, authorId, tenantId);

                if (customAttributes != null && !customAttributes.isEmpty()) {
                    metadata.customAttributes().putAll(customAttributes);
                }
                String s3Key = S3KeyUtil.generateKey(tenantId, metadata.fileId());

                PutObjectRequest putRequest = S3MetadataMapper.addMetadata(
                                PutObjectRequest.builder()
                                        .bucket(properties.bucketName())
                                        .key(s3Key)
                                        .contentType(contentType),
                                metadata)
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));

                publishStorageEvent(metadata);

                log.info(
                        "File stored successfully in S3 - fileId: {}, size: {} bytes, tenant: {}",
                        metadata.fileId(),
                        fileBytes.length,
                        tenantId);

                return metadata;

            } catch (IOException e) {
                log.error("IO error during S3 file storage - fileName: {}, tenant: {}", fileName, tenantId, e);
                throw new FileStorageException("Failed to read file content: " + e.getMessage(), e);
            } catch (S3Exception e) {
                log.error("S3 error during file storage - fileName: {}, tenant: {}", fileName, tenantId, e);
                throw new FileStorageException("S3 storage operation failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during S3 file storage - fileName: {}, tenant: {}", fileName, tenantId, e);
                throw new FileStorageException("File storage operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<FileContent>> retrieveFile(UUID fileId, UUID tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting S3 file retrieval operation - fileId: {}, tenantId: {}", fileId, tenantId);

                String s3Key = S3KeyUtil.generateKey(tenantId, fileId);

                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(properties.bucketName())
                        .key(s3Key)
                        .build();

                var responseInputStream = s3Client.getObject(getRequest);
                GetObjectResponse response = responseInputStream.response();

                FileMetadata metadata = S3MetadataMapper.extractMetadata(response, s3Key);
                FileContent fileContent = new FileContent(metadata, responseInputStream);

                publishAccessEvent(fileId, tenantId, metadata.authorId());

                log.info("File retrieved successfully from S3 - fileId: {}, tenant: {}", fileId, tenantId);

                return Optional.of(fileContent);

            } catch (NoSuchKeyException e) {
                log.debug("File not found in S3 - fileId: {}, tenantId: {}", fileId, tenantId);
                return Optional.empty();
            } catch (S3Exception e) {
                log.error("S3 error during file retrieval - fileId: {}, tenant: {}", fileId, tenantId, e);
                throw new FileStorageException("S3 retrieval operation failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during S3 file retrieval - fileId: {}, tenant: {}", fileId, tenantId, e);
                throw new FileStorageException("File retrieval operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteFile(UUID fileId, UUID tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting S3 file deletion operation - fileId: {}, tenantId: {}", fileId, tenantId);

                String s3Key = S3KeyUtil.generateKey(tenantId, fileId);

                // Check if file exists and get metadata first
                HeadObjectResponse headResponse;
                try {
                    headResponse = s3Client.headObject(HeadObjectRequest.builder()
                            .bucket(properties.bucketName())
                            .key(s3Key)
                            .build());
                } catch (NoSuchKeyException e) {
                    log.debug("File not found for deletion - fileId: {}, tenantId: {}", fileId, tenantId);
                    return false;
                }

                // Extract author ID from metadata for event
                GetObjectResponse getResponse = GetObjectResponse.builder()
                        .metadata(headResponse.metadata())
                        .build();
                FileMetadata metadata = S3MetadataMapper.extractMetadata(getResponse, s3Key);

                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(properties.bucketName())
                        .key(s3Key)
                        .build();

                s3Client.deleteObject(deleteRequest);

                publishDeleteEvent(fileId, tenantId, metadata.authorId());

                log.info("File deleted successfully from S3 - fileId: {}, tenant: {}", fileId, tenantId);

                return true;

            } catch (S3Exception e) {
                log.error("S3 error during file deletion - fileId: {}, tenant: {}", fileId, tenantId, e);
                throw new FileStorageException("S3 deletion operation failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during S3 file deletion - fileId: {}, tenant: {}", fileId, tenantId, e);
                throw new FileStorageException("File deletion operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<FileMetadata>> getFileMetadata(UUID fileId, UUID tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting S3 file metadata retrieval - fileId: {}, tenantId: {}", fileId, tenantId);

                String s3Key = S3KeyUtil.generateKey(tenantId, fileId);

                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(properties.bucketName())
                        .key(s3Key)
                        .build();

                HeadObjectResponse response = s3Client.headObject(headRequest);

                // Convert HeadObjectResponse to GetObjectResponse for metadata extraction
                GetObjectResponse getResponse = GetObjectResponse.builder()
                        .metadata(response.metadata())
                        .contentLength(response.contentLength())
                        .lastModified(response.lastModified())
                        .build();

                FileMetadata metadata = S3MetadataMapper.extractMetadata(getResponse, s3Key);

                log.debug("File metadata retrieved successfully from S3 - fileId: {}, tenant: {}", fileId, tenantId);

                return Optional.of(metadata);

            } catch (NoSuchKeyException e) {
                log.debug("File metadata not found in S3 - fileId: {}, tenantId: {}", fileId, tenantId);
                return Optional.empty();
            } catch (S3Exception e) {
                log.error("S3 error during metadata retrieval - fileId: {}, tenant: {}", fileId, tenantId, e);
                throw new FileStorageException("S3 metadata retrieval failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error(
                        "Unexpected error during S3 metadata retrieval - fileId: {}, tenant: {}", fileId, tenantId, e);
                throw new FileStorageException("Metadata retrieval operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<List<FileMetadata>> listFiles(UUID tenantId, FileFilter filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting S3 file listing operation - tenantId: {}", tenantId);

                String prefix = "files/" + tenantId + "/";

                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(properties.bucketName())
                        .prefix(prefix)
                        .build();

                List<FileMetadata> fileList = new ArrayList<>();

                ListObjectsV2Response response;
                do {
                    response = s3Client.listObjectsV2(listRequest);

                    for (S3Object s3Object : response.contents()) {
                        try {
                            // Get metadata for each object
                            HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
                                    .bucket(properties.bucketName())
                                    .key(s3Object.key())
                                    .build());

                            GetObjectResponse getResponse = GetObjectResponse.builder()
                                    .metadata(headResponse.metadata())
                                    .contentLength(headResponse.contentLength())
                                    .lastModified(headResponse.lastModified())
                                    .build();

                            FileMetadata metadata = S3MetadataMapper.extractMetadata(getResponse, s3Object.key());

                            if (filter == null || filter.matches(metadata)) {
                                fileList.add(metadata);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to extract metadata for S3 object: {}", s3Object.key(), e);
                        }
                    }

                    listRequest = listRequest.toBuilder()
                            .continuationToken(response.nextContinuationToken())
                            .build();

                } while (response.isTruncated());

                log.info("File listing completed for S3 - tenant: {}, found: {} files", tenantId, fileList.size());

                return fileList;

            } catch (S3Exception e) {
                log.error("S3 error during file listing - tenant: {}", tenantId, e);
                throw new FileStorageException("S3 file listing failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during S3 file listing - tenant: {}", tenantId, e);
                throw new FileStorageException("File listing operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Long> getTotalStorageUsed(UUID tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Calculating total storage used for tenant: {}", tenantId);

                String prefix = "files/" + tenantId + "/";

                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(properties.bucketName())
                        .prefix(prefix)
                        .build();

                long totalSize = 0L;

                ListObjectsV2Response response;
                do {
                    response = s3Client.listObjectsV2(listRequest);

                    totalSize += response.contents().stream()
                            .mapToLong(S3Object::size)
                            .sum();

                    listRequest = listRequest.toBuilder()
                            .continuationToken(response.nextContinuationToken())
                            .build();

                } while (response.isTruncated());

                log.debug("Total storage used for tenant {}: {} bytes", tenantId, totalSize);

                return totalSize;

            } catch (S3Exception e) {
                log.error("S3 error during storage calculation - tenant: {}", tenantId, e);
                throw new FileStorageException("S3 storage calculation failed: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error during storage calculation - tenant: {}", tenantId, e);
                throw new FileStorageException("Storage calculation operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<List<FileMetadata>> getFilesByAuthor(UUID authorId, UUID tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting S3 files by author retrieval - authorId: {}, tenantId: {}", authorId, tenantId);

                // List all files for tenant and filter by author
                return listFiles(tenantId, null).join().stream()
                        .filter(metadata -> authorId.equals(metadata.authorId()))
                        .toList();

            } catch (Exception e) {
                log.error(
                        "Error during S3 files by author retrieval - authorId: {}, tenant: {}", authorId, tenantId, e);
                throw new FileStorageException("Files by author retrieval failed: " + e.getMessage(), e);
            }
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
            throw new IllegalArgumentException("File content cannot be null");
        }

        byte[] fileBytes = content.readAllBytes();

        if (fileBytes.length > properties.maxFileSize()) {
            throw new FileStorageException(String.format(
                    "File size (%d bytes) exceeds maximum allowed size (%d bytes) for file: %s",
                    fileBytes.length, properties.maxFileSize(), fileName));
        }

        if (fileBytes.length == 0) {
            throw new FileStorageException("File content cannot be empty for file: " + fileName);
        }

        return fileBytes;
    }

    private void publishStorageEvent(FileMetadata metadata) {
        try {
            eventPublisher.publish(new FileStoredEvent(metadata));
        } catch (Exception e) {
            log.warn("Failed to publish FileStoredEvent for file: {}", metadata.fileId(), e);
        }
    }

    private void publishAccessEvent(UUID fileId, UUID tenantId, UUID authorId) {
        try {
            eventPublisher.publish(new FileAccessedEvent(fileId, tenantId, authorId, java.time.Instant.now()));
        } catch (Exception e) {
            log.warn("Failed to publish FileAccessedEvent for file: {}", fileId, e);
        }
    }

    private void publishDeleteEvent(UUID fileId, UUID tenantId, UUID authorId) {
        try {
            eventPublisher.publish(new FileDeletedEvent(fileId, tenantId, authorId));
        } catch (Exception e) {
            log.warn("Failed to publish FileDeletedEvent for file: {}", fileId, e);
        }
    }
}
