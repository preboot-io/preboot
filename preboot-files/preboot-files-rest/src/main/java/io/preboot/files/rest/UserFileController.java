package io.preboot.files.rest;

import io.preboot.files.api.FileFilter;
import io.preboot.files.api.FileStorageService;
import io.preboot.files.model.FileContent;
import io.preboot.files.model.FileMetadata;
import io.preboot.files.rest.dto.FileListResponse;
import io.preboot.files.rest.dto.FileMetadataResponse;
import io.preboot.securedata.context.SecurityContext;
import io.preboot.securedata.context.SecurityContextProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class UserFileController {

    private final FileStorageService fileStorageService;
    private final SecurityContextProvider securityContextProvider;

    private SecurityContext getSecurityContext() {
        SecurityContext context = securityContextProvider.getCurrentContext();
        if (context == null || context.getUserId() == null || context.getTenantId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Security context not available or incomplete.");
        }
        return context;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload a file for the current user within their active tenant.")
    public CompletableFuture<ResponseEntity<FileMetadataResponse>> uploadFile(
            @Parameter(description = "File to upload.", required = true) @RequestParam("file") MultipartFile file) {

        SecurityContext context = getSecurityContext();
        UUID currentUserId = context.getUserId();
        UUID currentTenantId = context.getTenantId();

        log.info(
                "User file upload request - tenant: {}, author: {}, filename: {}, size: {} bytes",
                currentTenantId,
                currentUserId,
                file.getOriginalFilename(),
                file.getSize());

        try {
            return fileStorageService
                    .storeFile(
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getInputStream(),
                            currentUserId,
                            currentTenantId)
                    .thenApply(metadata -> {
                        FileMetadataResponse response = FileMetadataResponse.from(metadata);
                        String locationHeader =
                                String.format("/api/files/%s", metadata.fileId().toString());
                        return ResponseEntity.status(HttpStatus.CREATED)
                                .header(HttpHeaders.LOCATION, locationHeader)
                                .body(response);
                    });
        } catch (IOException e) {
            log.error(
                    "Failed to read multipart file content for user {} in tenant {}",
                    currentUserId,
                    currentTenantId,
                    e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file content", e);
        }
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Download a specific file owned by the current user from their active tenant.")
    public CompletableFuture<ResponseEntity<InputStreamResource>> downloadFile(
            @Parameter(description = "ID of the file to download.", in = ParameterIn.PATH, required = true)
                    @PathVariable
                    @NotNull UUID fileId) {

        SecurityContext context = getSecurityContext();
        UUID currentUserId = context.getUserId();
        UUID currentTenantId = context.getTenantId();

        log.debug(
                "User file download request - tenant: {}, fileId: {}, requesting user: {}",
                currentTenantId,
                fileId,
                currentUserId);

        return fileStorageService.retrieveFile(fileId, currentTenantId).thenApply(fileContentOpt -> {
            if (fileContentOpt.isEmpty()) {
                log.debug(
                        "File not found for user {} in tenant {} - fileId: {}", currentUserId, currentTenantId, fileId);
                return ResponseEntity.notFound().build();
            }

            FileContent fileContent = fileContentOpt.get();
            FileMetadata metadata = fileContent.metadata();

            if (!metadata.tenantId().equals(currentTenantId)
                    || !metadata.authorId().equals(currentUserId)) {
                log.warn(
                        "User {} (tenant {}) attempted to access file {} owned by {} in tenant {}",
                        currentUserId,
                        currentTenantId,
                        fileId,
                        metadata.authorId(),
                        metadata.tenantId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            InputStreamResource resource = new InputStreamResource(fileContent.contentStream());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(metadata.contentType()))
                    .contentLength(metadata.fileSize())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.fileName() + "\"")
                    .body(resource);
        });
    }

    @GetMapping("/{fileId}/metadata")
    @Operation(summary = "Get metadata for a specific file owned by the current user from their active tenant.")
    public CompletableFuture<ResponseEntity<FileMetadataResponse>> getFileMetadata(
            @Parameter(description = "ID of the file.", in = ParameterIn.PATH, required = true) @PathVariable @NotNull UUID fileId) {

        SecurityContext context = getSecurityContext();
        UUID currentUserId = context.getUserId();
        UUID currentTenantId = context.getTenantId();

        log.debug(
                "User file metadata request - tenant: {}, fileId: {}, requesting user: {}",
                currentTenantId,
                fileId,
                currentUserId);

        return fileStorageService.getFileMetadata(fileId, currentTenantId).thenApply(metadataOpt -> {
            if (metadataOpt.isEmpty()) {
                log.debug(
                        "Metadata not found for user {} in tenant {} - fileId: {}",
                        currentUserId,
                        currentTenantId,
                        fileId);
                return ResponseEntity.notFound().build();
            }
            FileMetadata metadata = metadataOpt.get();
            if (!metadata.tenantId().equals(currentTenantId)
                    || !metadata.authorId().equals(currentUserId)) {
                log.warn(
                        "User {} (tenant {}) attempted to access metadata for file {} owned by {} in tenant {}",
                        currentUserId,
                        currentTenantId,
                        fileId,
                        metadata.authorId(),
                        metadata.tenantId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            FileMetadataResponse response = FileMetadataResponse.from(metadata);
            return ResponseEntity.ok(response);
        });
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Delete a specific file owned by the current user from their active tenant.")
    public CompletableFuture<ResponseEntity<Void>> deleteFile(
            @Parameter(description = "ID of the file to delete.", in = ParameterIn.PATH, required = true)
                    @PathVariable
                    @NotNull UUID fileId) {

        SecurityContext context = getSecurityContext();
        UUID currentUserId = context.getUserId();
        UUID currentTenantId = context.getTenantId();

        log.info(
                "User file deletion request - tenant: {}, fileId: {}, requesting user: {}",
                currentTenantId,
                fileId,
                currentUserId);

        return fileStorageService.getFileMetadata(fileId, currentTenantId).thenCompose(metadataOpt -> {
            if (metadataOpt.isEmpty()) {
                log.debug(
                        "File not found for deletion for user {} in tenant {} - fileId: {}",
                        currentUserId,
                        currentTenantId,
                        fileId);
                return CompletableFuture.completedFuture(
                        ResponseEntity.notFound().<Void>build());
            }
            FileMetadata metadata = metadataOpt.get();
            if (!metadata.tenantId().equals(currentTenantId)
                    || !metadata.authorId().equals(currentUserId)) {
                log.warn(
                        "User {} (tenant {}) attempted to delete file {} owned by {} in tenant {}",
                        currentUserId,
                        currentTenantId,
                        fileId,
                        metadata.authorId(),
                        metadata.tenantId());
                return CompletableFuture.completedFuture(
                        ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build());
            }

            return fileStorageService.deleteFile(fileId, currentTenantId).thenApply(deleted -> {
                if (Boolean.TRUE.equals(deleted)) {
                    log.info(
                            "File deleted successfully by user {} in tenant {} - fileId: {}",
                            currentUserId,
                            currentTenantId,
                            fileId);
                    return ResponseEntity.noContent().<Void>build();
                } else {
                    log.warn(
                            "File {} in tenant {} was found by metadata check but delete operation failed for user {}",
                            fileId,
                            currentTenantId,
                            currentUserId);
                    return ResponseEntity.notFound().<Void>build();
                }
            });
        });
    }

    @GetMapping
    @Operation(
            summary =
                    "List files owned by the current user within their active tenant, optionally filtered by content type.")
    public CompletableFuture<ResponseEntity<FileListResponse>> listFiles(
            @Parameter(description = "Filter by content type.", in = ParameterIn.QUERY) @RequestParam(required = false)
                    String contentType) {

        SecurityContext context = getSecurityContext();
        UUID currentUserId = context.getUserId();
        UUID currentTenantId = context.getTenantId();

        log.debug(
                "User file list request - tenant: {}, requesting user: {}, contentType: {}",
                currentTenantId,
                currentUserId,
                contentType);

        FileFilter userFilter = FileFilter.byAuthor(currentUserId);
        FileFilter finalFilter = userFilter;

        if (contentType != null && !contentType.trim().isEmpty()) {
            finalFilter = finalFilter.and(FileFilter.byContentType(contentType));
        }

        return fileStorageService.listFiles(currentTenantId, finalFilter).thenApply(metadataList -> {
            List<FileMetadataResponse> responseList =
                    metadataList.stream().map(FileMetadataResponse::from).toList();
            FileListResponse response = FileListResponse.of(responseList, currentTenantId);
            return ResponseEntity.ok(response);
        });
    }
}
