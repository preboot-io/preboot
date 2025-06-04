package io.preboot.core.colections;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TTLMap<K, V> {
    private static final ScheduledExecutorService SHARED_CLEANUP_SERVICE;
    private static final Set<TTLMap<?, ?>> INSTANCES = ConcurrentHashMap.newKeySet();

    static {
        SHARED_CLEANUP_SERVICE = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("TTLMap-Cleanup-Thread");
            return thread;
        });

        // Schedule cleanup every second for all instances
        SHARED_CLEANUP_SERVICE.scheduleAtFixedRate(
                () -> {
                    INSTANCES.forEach(TTLMap::removeExpiredEntries);
                },
                1, // initial delay
                1, // period
                TimeUnit.SECONDS);
    }

    private final Map<K, TimedEntry<V>> store;
    private final long defaultTTL;

    private record TimedEntry<V>(V value, Instant expiryTime) {
        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }

    public TTLMap(long defaultTTLSeconds) {
        this.store = new ConcurrentHashMap<>();
        this.defaultTTL = defaultTTLSeconds;
        INSTANCES.add(this);
    }

    public void put(K key, V value) {
        put(key, value, defaultTTL);
    }

    public void put(K key, V value, long ttlSeconds) {
        Instant expiryTime = Instant.now().plusSeconds(ttlSeconds);
        store.put(key, new TimedEntry<>(value, expiryTime));
    }

    public V get(K key) {
        TimedEntry<V> entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            store.remove(key);
            return null;
        }
        return entry.value();
    }

    public void remove(K key) {
        store.remove(key);
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        removeExpiredEntries();
        return store.size();
    }

    private void removeExpiredEntries() {
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public void close() {
        INSTANCES.remove(this);
        store.clear();
    }

    // Shutdown hook for the entire service
    public static void shutdownCleanupService() {
        INSTANCES.clear();
        SHARED_CLEANUP_SERVICE.shutdown();
    }

    // to ensure cleanup on JVM shutdown
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(TTLMap::shutdownCleanupService));
    }
}
