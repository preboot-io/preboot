package io.preboot.query.web.spi;

import java.util.UUID;

public record UserContext(UUID userId, UUID tenantId) {}
