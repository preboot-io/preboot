package io.preboot.files.rest.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class FileListResponse {

    private final List<FileMetadataResponse> files;
    private final long totalCount;
    private final UUID tenantId;

    public static FileListResponse of(List<FileMetadataResponse> files, UUID tenantId) {
        return FileListResponse.builder()
                .files(files)
                .totalCount(files.size())
                .tenantId(tenantId)
                .build();
    }
}
