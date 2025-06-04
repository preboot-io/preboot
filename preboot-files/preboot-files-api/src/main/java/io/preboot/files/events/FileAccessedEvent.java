package io.preboot.files.events;

import java.time.Instant;
import java.util.UUID;

public record FileAccessedEvent(UUID fileId, UUID tenantId, UUID accessorId, Instant accessTime) {}
