package io.preboot.files.events;

import java.util.UUID;

public record FileDeletedEvent(UUID fileId, UUID tenantId, UUID authorId) {}
