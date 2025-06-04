package io.preboot.query;

import java.util.UUID;

/** Interface for entities that have a UUID identifier in addition to their database ID. */
public interface HasUuid {
    UUID getUuid();

    void setUuid(UUID uuid);
}
