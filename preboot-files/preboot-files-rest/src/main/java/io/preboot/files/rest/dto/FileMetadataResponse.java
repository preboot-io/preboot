package io.preboot.files.rest.dto;

import io.preboot.files.model.FileMetadata;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class FileMetadataResponse {

    @NotNull private final UUID fileId;

    @NotBlank
    @Size(max = 255)
    private final String fileName;

    @NotBlank
    private final String contentType;

    private final long fileSize;

    @NotNull private final UUID authorId;

    @NotNull private final UUID tenantId;

    private final Instant createdAt;
    private final Instant lastModified;

    private final Map<String, String> customAttributes;

    public static FileMetadataResponse from(FileMetadata metadata) {
        return FileMetadataResponse.builder()
                .fileId(metadata.fileId())
                .fileName(metadata.fileName())
                .contentType(metadata.contentType())
                .fileSize(metadata.fileSize())
                .authorId(metadata.authorId())
                .tenantId(metadata.tenantId())
                .createdAt(metadata.createdAt())
                .lastModified(metadata.lastModified())
                .customAttributes(metadata.customAttributes())
                .build();
    }
}
