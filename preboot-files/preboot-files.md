---
layout: documentation
title: "Files Management"
subtitle: "Multi-tenant file storage system with pluggable storage backends and event-driven architecture."
permalink: /docs/backend/modules/files/
section: backend
---
# preboot-files

## Overview
The `preboot-files` module provides a comprehensive file management system with multi-tenant isolation, pluggable storage backends, and event-driven architecture. It supports multiple storage implementations including in-memory, AWS S3, and S3-compatible services like OVH Cloud Storage or MinIO.

## Features
- Multi-tenant file isolation
- Async file operations with CompletableFuture
- Pluggable storage backends (in-memory, S3, S3-compatible)
- File metadata management with custom attributes
- REST API with Swagger documentation
- Event publishing for file lifecycle
- File size limits and validation
- Author-based file filtering
- Storage usage tracking

## Module Structure

The module consists of several submodules:

- **preboot-files-api** - Core interfaces and models
- **preboot-files-in-memory** - In-memory storage implementation
- **preboot-files-s3** - AWS S3 and S3-compatible storage implementation  
- **preboot-files-rest** - REST API endpoints

## Installation

### Core API
```xml
<dependency>
    <groupId>io.preboot</groupId>
    <artifactId>preboot-files-api</artifactId>
    <version>{version}</version>
</dependency>
```

### In-Memory Implementation
```xml
<dependency>
    <groupId>io.preboot</groupId>
    <artifactId>preboot-files-in-memory</artifactId>
    <version>{version}</version>
</dependency>
```

### S3 Implementation
```xml
<dependency>
    <groupId>io.preboot</groupId>
    <artifactId>preboot-files-s3</artifactId>
    <version>{version}</version>
</dependency>
```

### REST API
```xml
<dependency>
    <groupId>io.preboot</groupId>
    <artifactId>preboot-files-rest</artifactId>
    <version>{version}</version>
</dependency>
```

## Core API Components

### FileStorageService
The main service interface for file operations:

```java
@Service
public class DocumentService {
    private final FileStorageService fileStorageService;

    public DocumentService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public CompletableFuture<FileMetadata> uploadDocument(String fileName, 
                                                        String contentType,
                                                        InputStream content,
                                                        UUID authorId,
                                                        UUID tenantId) {
        return fileStorageService.storeFile(fileName, contentType, content, authorId, tenantId);
    }

    public CompletableFuture<Optional<FileContent>> downloadDocument(UUID fileId, UUID tenantId) {
        return fileStorageService.retrieveFile(fileId, tenantId);
    }

    public CompletableFuture<List<FileMetadata>> listUserDocuments(UUID userId, UUID tenantId) {
        return fileStorageService.getFilesByAuthor(userId, tenantId);
    }
}
```

### File Models

#### FileMetadata
Contains file information and custom attributes:

```java
public record FileMetadata(
    UUID fileId,
    String fileName,
    String contentType,
    long fileSize,
    UUID authorId,
    UUID tenantId,
    Instant createdAt,
    Instant lastModified,
    Map<String, String> customAttributes
) {
    public static FileMetadata create(String fileName, String contentType, 
                                    long fileSize, UUID authorId, UUID tenantId) {
        return new FileMetadata(
            UUID.randomUUID(),
            fileName,
            contentType,
            fileSize,
            authorId,
            tenantId,
            Instant.now(),
            Instant.now(),
            new HashMap<>()
        );
    }
}
```

#### FileContent
Combines metadata with file content stream:

```java
public record FileContent(
    FileMetadata metadata,
    InputStream content
) {}
```

### File Filtering
Use `FileFilter` to filter files in listings:

```java
public class DocumentFilter {
    
    public static FileFilter byContentType(String contentType) {
        return metadata -> contentType.equals(metadata.contentType());
    }
    
    public static FileFilter byCustomAttribute(String key, String value) {
        return metadata -> value.equals(metadata.customAttributes().get(key));
    }
    
    public static FileFilter combine(FileFilter... filters) {
        return metadata -> Arrays.stream(filters).allMatch(filter -> filter.matches(metadata));
    }
}

// Usage
FileFilter pdfFilter = DocumentFilter.byContentType("application/pdf");
List<FileMetadata> pdfFiles = fileStorageService.listFiles(tenantId, pdfFilter).join();
```

## Storage Implementations

### In-Memory Storage
Best for development and testing:

```yaml
preboot:
  files:
    in-memory:
      max-file-size: 10485760  # 10MB
```

### S3 Storage
Production-ready with AWS S3 or S3-compatible services:

```yaml
preboot:
  files:
    s3:
      bucket-name: my-app-files
      region: us-east-1
      max-file-size: 52428800  # 50MB
      # For S3-compatible services:
      # endpoint-url: https://s3.gra.io.cloud.ovh.net
      # access-key-id: YOUR_ACCESS_KEY
      # secret-access-key: YOUR_SECRET_KEY
```

### Multiple Storage Backends
The system supports only one storage implementation at a time. Choose based on your deployment environment:

```yaml
# Development
spring:
  profiles:
    active: dev
    
---
spring:
  config:
    activate:
      on-profile: dev
      
preboot:
  files:
    in-memory:
      max-file-size: 10485760

---
spring:
  config:
    activate:
      on-profile: prod
      
preboot:
  files:
    s3:
      bucket-name: ${S3_BUCKET_NAME}
      region: ${AWS_REGION}
```

## Events

The system publishes events for file lifecycle operations:

### File Events
```java
@EventHandler
public class FileEventHandler {
    
    @EventHandler
    public void onFileStored(FileStoredEvent event) {
        FileMetadata metadata = event.metadata();
        log.info("File stored: {} by user {}", metadata.fileName(), metadata.authorId());
        // Send notification, update audit log, etc.
    }
    
    @EventHandler
    public void onFileAccessed(FileAccessedEvent event) {
        log.info("File {} accessed by user {}", event.fileId(), event.authorId());
        // Track access patterns, analytics, etc.
    }
    
    @EventHandler
    public void onFileDeleted(FileDeletedEvent event) {
        log.info("File {} deleted from tenant {}", event.fileId(), event.tenantId());
        // Cleanup related data, notify users, etc.
    }
}
```

## REST API

### File Upload
```http
POST /api/files/upload
Content-Type: multipart/form-data

file: [binary content]
```

### File Download
```http
GET /api/files/{fileId}
```

### File Metadata
```http
GET /api/files/{fileId}/metadata
```

### List Files
```http
GET /api/files?author={authorId}&contentType={type}&customAttribute={key}={value}
```

### Delete File
```http
DELETE /api/files/{fileId}
```

### Storage Usage
```http
GET /api/files/storage/usage
```

## Configuration Examples

### Basic In-Memory Setup
```yaml
preboot:
  files:
    in-memory:
      max-file-size: 10485760  # 10MB
```

### AWS S3 with IAM Roles
```yaml
preboot:
  files:
    s3:
      bucket-name: my-production-files
      region: us-east-1
      max-file-size: 104857600  # 100MB
```

### OVH Cloud Storage
```yaml
preboot:
  files:
    s3:
      bucket-name: my-ovh-container
      region: gra
      endpoint-url: https://s3.gra.io.cloud.ovh.net
      access-key-id: ${OVH_ACCESS_KEY}
      secret-access-key: ${OVH_SECRET_KEY}
      path-style-access-enabled: true  # Required for OVH
      max-file-size: 52428800  # 50MB
```

### MinIO (Development)
```yaml
preboot:
  files:
    s3:
      bucket-name: dev-files
      region: us-east-1
      endpoint-url: http://localhost:9000
      access-key-id: minioadmin
      secret-access-key: minioadmin
      path-style-access-enabled: true  # Required for MinIO
      max-file-size: 10485760  # 10MB
```

## Security & Multi-Tenancy

### Tenant Isolation
All file operations are automatically isolated by tenant:

```java
// Files are stored with tenant-specific keys
// S3 key format: files/{tenantId}/{fileId}
// In-memory storage uses tenant-specific maps

// Cross-tenant access is automatically prevented
Optional<FileContent> file = fileStorageService.retrieveFile(fileId, wrongTenantId);
// Returns Optional.empty() even if file exists for different tenant
```

### Authorization
The system integrates with PreBoot security context:

```java
@RestController
public class FileController {
    
    @PostMapping("/upload")
    public CompletableFuture<FileMetadata> upload(@RequestParam MultipartFile file) {
        SecurityContext context = securityContextProvider.getSecurityContext();
        
        return fileStorageService.storeFile(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getInputStream(),
            context.getUserId(),     // Automatic author assignment
            context.getTenantId()    // Automatic tenant isolation
        );
    }
}
```

## Error Handling

### Common Exceptions
```java
try {
    FileMetadata metadata = fileStorageService.storeFile(fileName, contentType, content, authorId, tenantId).join();
} catch (CompletionException e) {
    if (e.getCause() instanceof FileStorageException) {
        FileStorageException fse = (FileStorageException) e.getCause();
        if (fse.getMessage().contains("exceeds maximum allowed size")) {
            // Handle file too large
        } else if (fse.getMessage().contains("S3")) {
            // Handle S3-specific errors
        }
    }
}
```

### Validation Errors
```java
// File size validation
preboot:
  files:
    s3:
      max-file-size: 52428800  # 50MB limit

// Results in FileStorageException if exceeded
```

## Performance Considerations

### Async Operations
All file operations return `CompletableFuture` for non-blocking execution:

```java
// Non-blocking file operations
CompletableFuture<FileMetadata> uploadFuture = fileStorageService.storeFile(...);
CompletableFuture<Optional<FileContent>> downloadFuture = fileStorageService.retrieveFile(...);

// Combine multiple operations
CompletableFuture<String> combinedOperation = uploadFuture
    .thenCompose(metadata -> fileStorageService.retrieveFile(metadata.fileId(), tenantId))
    .thenApply(content -> "File processed: " + content.orElse(null));
```

### Storage Backend Selection
- **In-Memory**: Best for development, testing, and temporary files
- **S3**: Production-ready, scalable, with backup and versioning
- **S3-Compatible**: Cost-effective alternatives (OVH, MinIO, DigitalOcean)

### File Size Limits
Configure appropriate limits based on your infrastructure:

```yaml
# Development (smaller limits)
preboot:
  files:
    in-memory:
      max-file-size: 10485760  # 10MB

# Production (larger limits)
preboot:
  files:
    s3:
      max-file-size: 104857600  # 100MB
```

## Best Practices

### 1. File Metadata Usage
```java
// Add custom attributes for better file organization
Map<String, String> customAttributes = Map.of(
    "category", "invoice",
    "department", "finance",
    "quarter", "Q1-2024"
);

// Custom attributes are preserved across storage backends
FileMetadata metadata = FileMetadata.create(fileName, contentType, fileSize, authorId, tenantId);
metadata.customAttributes().putAll(customAttributes);
```

### 2. Event Handling
```java
@Component
public class FileAuditHandler {
    
    @EventHandler
    public void onFileStored(FileStoredEvent event) {
        // Audit trail
        auditService.logFileOperation("STORE", event.metadata());
    }
    
    @EventHandler
    public void onFileDeleted(FileDeletedEvent event) {
        // Cleanup related data
        cleanupService.removeFileReferences(event.fileId());
    }
}
```

### 3. Error Recovery
```java
public CompletableFuture<FileMetadata> uploadWithRetry(String fileName, InputStream content, 
                                                      UUID authorId, UUID tenantId) {
    return fileStorageService.storeFile(fileName, "application/octet-stream", content, authorId, tenantId)
        .exceptionally(throwable -> {
            log.warn("File upload failed, retrying: {}", throwable.getMessage());
            // Implement retry logic
            return fileStorageService.storeFile(fileName, "application/octet-stream", 
                                              content, authorId, tenantId).join();
        });
}
```

### 4. File Cleanup
```java
@Scheduled(fixedRate = 3600000) // Every hour
public void cleanupTemporaryFiles() {
    FileFilter tempFilter = metadata -> 
        metadata.customAttributes().get("temporary") != null;
    
    fileStorageService.listFiles(tenantId, tempFilter)
        .thenCompose(files -> {
            List<CompletableFuture<Boolean>> deletions = files.stream()
                .filter(metadata -> isExpired(metadata.createdAt()))
                .map(metadata -> fileStorageService.deleteFile(metadata.fileId(), metadata.tenantId()))
                .toList();
            return CompletableFuture.allOf(deletions.toArray(new CompletableFuture[0]));
        });
}
```

## Testing

### Unit Testing with Mock Storage
```java
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Test
    void shouldUploadDocument() {
        // Mock file storage
        when(fileStorageService.storeFile(any(), any(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockMetadata));
            
        // Test your service
        FileMetadata result = documentService.uploadDocument(file, authorId, tenantId).join();
        
        assertThat(result).isNotNull();
        verify(fileStorageService).storeFile(fileName, contentType, content, authorId, tenantId);
    }
}
```

### Integration Testing with TestContainers
```java
@Testcontainers
class S3FileStorageIntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"))
        .withServices(LocalStackContainer.Service.S3);

    @Test
    void shouldPerformFullFileLifecycle() {
        // Test with real S3 API using LocalStack
        FileMetadata stored = service.storeFile(fileName, contentType, content, authorId, tenantId).join();
        Optional<FileContent> retrieved = service.retrieveFile(stored.fileId(), tenantId).join();
        Boolean deleted = service.deleteFile(stored.fileId(), tenantId).join();
        
        assertThat(stored).isNotNull();
        assertThat(retrieved).isPresent();
        assertThat(deleted).isTrue();
    }
}
```

## AWS S3 Setup

### Required Permissions
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::your-bucket-name/*",
        "arn:aws:s3:::your-bucket-name"
      ]
    }
  ]
}
```

### IAM Role Configuration (Recommended)
```yaml
# Production setup with IAM roles (no credentials in config)
preboot:
  files:
    s3:
      bucket-name: ${S3_BUCKET_NAME}
      region: ${AWS_REGION}
      max-file-size: ${MAX_FILE_SIZE:104857600}
```

### Explicit Credentials (Development)
```yaml
preboot:
  files:
    s3:
      bucket-name: dev-files
      region: us-east-1
      access-key-id: ${AWS_ACCESS_KEY_ID}
      secret-access-key: ${AWS_SECRET_ACCESS_KEY}
      max-file-size: 10485760
```

## S3-Compatible Services

### OVH Cloud Storage
```yaml
preboot:
  files:
    s3:
      bucket-name: my-ovh-container
      region: gra
      endpoint-url: https://s3.gra.io.cloud.ovh.net
      access-key-id: ${OVH_ACCESS_KEY}
      secret-access-key: ${OVH_SECRET_KEY}
```

### MinIO (Self-Hosted)
```yaml
preboot:
  files:
    s3:
      bucket-name: minio-files
      region: us-east-1
      endpoint-url: http://localhost:9000
      access-key-id: minioadmin
      secret-access-key: minioadmin
      path-style-access-enabled: true  # Required for MinIO
```

### DigitalOcean Spaces
```yaml
preboot:
  files:
    s3:
      bucket-name: my-do-space
      region: nyc3
      endpoint-url: https://nyc3.digitaloceanspaces.com
      access-key-id: ${DO_SPACES_KEY}
      secret-access-key: ${DO_SPACES_SECRET}
      path-style-access-enabled: true  # Required for DigitalOcean Spaces
```

## REST API Usage

### Upload File
```bash
curl -X POST "http://localhost:8080/api/files/upload" \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -F "file=@document.pdf"
```

### Download File
```bash
curl -X GET "http://localhost:8080/api/files/{fileId}" \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -o downloaded-file.pdf
```

### List Files with Filters
```bash
curl -X GET "http://localhost:8080/api/files?author={authorId}&contentType=application/pdf" \
     -H "Authorization: Bearer YOUR_TOKEN"
```

## Monitoring & Observability

### Storage Metrics
```java
@Component
public class FileStorageMetrics {
    
    @EventHandler
    public void onFileStored(FileStoredEvent event) {
        meterRegistry.counter("files.stored", 
            "tenant", event.metadata().tenantId().toString(),
            "content_type", event.metadata().contentType())
            .increment();
            
        meterRegistry.summary("files.size")
            .record(event.metadata().fileSize());
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void reportStorageUsage() {
        tenantService.getAllTenants().forEach(tenantId -> {
            Long usage = fileStorageService.getTotalStorageUsed(tenantId).join();
            meterRegistry.gauge("files.storage_used", 
                Tags.of("tenant", tenantId.toString()), usage);
        });
    }
}
```

### Health Checks
```java
@Component
public class FileStorageHealthIndicator implements HealthIndicator {
    
    private final FileStorageService fileStorageService;
    
    @Override
    public Health health() {
        try {
            // Test storage connectivity
            UUID testTenant = UUID.randomUUID();
            Long usage = fileStorageService.getTotalStorageUsed(testTenant).get(5, TimeUnit.SECONDS);
            
            return Health.up()
                .withDetail("storage", "accessible")
                .withDetail("test_tenant", testTenant)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("storage", "inaccessible")
                .withException(e)
                .build();
        }
    }
}
```

## Thread Safety & Concurrency
- All `FileStorageService` implementations are thread-safe
- Async operations use `ForkJoinPool.commonPool()` by default
- Custom executors can be configured for async operations
- File metadata operations are atomic within each storage backend

## Limitations
- File content is loaded entirely into memory during upload
- No built-in file versioning (depends on storage backend features)
- No distributed file locks (each storage operation is independent)
- Custom attributes are limited by storage backend constraints (2KB for S3 metadata)
- In-memory storage is not persistent across application restarts

## Migration Between Storage Backends

When migrating from one storage backend to another, consider:

1. **Data Export/Import**: No built-in migration tools
2. **Configuration Changes**: Update application.yml properties
3. **Dependency Updates**: Change Maven dependencies
4. **Testing**: Verify functionality with new backend
5. **Event Compatibility**: All backends publish identical events

## Troubleshooting

### Common Issues

**File Not Found**
```java
Optional<FileContent> content = fileStorageService.retrieveFile(fileId, tenantId).join();
if (content.isEmpty()) {
    // File doesn't exist or wrong tenant
}
```

**Storage Quota Exceeded** (S3)
```bash
# Check AWS S3 bucket policies and quotas
# Monitor storage usage with metrics
```

**Connection Issues** (S3-Compatible)
```yaml
# For MinIO and other S3-compatible services, enable path-style access:
preboot:
  files:
    s3:
      path-style-access-enabled: true  # Required for MinIO, OVH, DigitalOcean Spaces
```

**File Size Limits**
```java
// FileStorageException: "File size (X bytes) exceeds maximum allowed size (Y bytes)"
// Increase max-file-size in configuration
```

### Debug Logging
```yaml
logging:
  level:
    io.preboot.files: DEBUG
    software.amazon.awssdk: DEBUG  # For S3 debugging
```