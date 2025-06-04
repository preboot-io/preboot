package io.preboot.files.events;

import io.preboot.files.model.FileMetadata;

public record FileStoredEvent(FileMetadata metadata) {}
