package io.preboot.files.api;

import io.preboot.files.model.FileMetadata;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public interface FileFilter {
    boolean matches(FileMetadata metadata);

    static FileFilter byContentType(String contentType) {
        return metadata -> Objects.equals(metadata.contentType(), contentType);
    }

    static FileFilter byAuthor(UUID authorId) {
        return metadata -> Objects.equals(metadata.authorId(), authorId);
    }

    static FileFilter byDateRange(Instant from, Instant to) {
        return metadata ->
                !metadata.createdAt().isBefore(from) && !metadata.createdAt().isAfter(to);
    }

    default FileFilter and(FileFilter other) {
        return metadata -> this.matches(metadata) && other.matches(metadata);
    }

    default FileFilter or(FileFilter other) {
        return metadata -> this.matches(metadata) || other.matches(metadata);
    }
}
