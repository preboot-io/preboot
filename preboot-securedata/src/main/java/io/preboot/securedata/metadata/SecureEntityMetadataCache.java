package io.preboot.securedata.metadata;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

// Metadata cache to avoid repeated reflection
@Component
public class SecureEntityMetadataCache {
    private final ConcurrentMap<Class<?>, SecureEntityMetadata<?>> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> SecureEntityMetadata<T> get(Class<T> entityType) {
        return (SecureEntityMetadata<T>)
                cache.computeIfAbsent(entityType, type -> new SecureEntityMetadata<>(entityType));
    }
}
