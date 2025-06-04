package io.preboot.core.concurent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A utility class that provides synchronization of operations based on a key. This helps prevent race conditions when
 * multiple threads need to perform operations on the same resource identified by the key.
 *
 * <p>This implementation uses ReentrantLock instead of synchronized blocks for better compatibility with virtual
 * threads introduced in Java 21.
 */
public class AccessSynchronizer {

    private final Map<Object, CounterLock> locks = new ConcurrentHashMap<>();

    /**
     * Executes the provided supplier within a locked block based on the given key. This ensures that only one thread at
     * a time can execute code for the same key.
     *
     * @param key The key to synchronize on
     * @param supplier The operation to execute
     * @param <K> The type of the key
     * @param <V> The return type of the supplier
     * @return The result of the supplier execution
     */
    public <K, V> V synchronize(K key, Supplier<V> supplier) {
        CounterLock lock = locks.compute(key, (k, v) -> v == null ? new CounterLock() : v.increment());

        Lock reentrantLock = lock.getLock();
        reentrantLock.lock();
        try {
            return supplier.get();
        } finally {
            reentrantLock.unlock();
            if (lock.decrement() == 0) {
                // Remove the lock only if the key still points to the same lock
                locks.remove(key, lock);
            }
        }
    }

    /**
     * Executes the provided runnable within a locked block based on the given key. This ensures that only one thread at
     * a time can execute code for the same key.
     *
     * @param key The key to synchronize on
     * @param runnable The operation to execute
     * @param <K> The type of the key
     */
    public <K> void synchronizeVoid(K key, Runnable runnable) {
        synchronize(key, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Creates a composite key from multiple objects. This is useful when synchronization should be based on a
     * combination of values rather than a single value.
     *
     * @param values Objects to combine into a composite key
     * @return A composite key that encapsulates all the provided values
     */
    public static CompositeKey compositeKey(Object... values) {
        return new CompositeKey(values);
    }

    /**
     * An internal class that maintains a counter for active synchronization requests and a ReentrantLock for thread
     * synchronization.
     */
    private static final class CounterLock {
        private final AtomicInteger remainingUsers = new AtomicInteger(1);
        private final Lock lock = new ReentrantLock(true); // Fair lock for better predictability

        private CounterLock increment() {
            // Return a new CounterLock if this one was about to be removed
            // This prevents issues with locks being removed while still needed
            return remainingUsers.getAndIncrement() == 0 ? new CounterLock() : this;
        }

        private int decrement() {
            return remainingUsers.decrementAndGet();
        }

        private Lock getLock() {
            return lock;
        }
    }

    /**
     * A class that encapsulates multiple objects into a single key for synchronization. It correctly implements
     * hashCode and equals to ensure proper key comparison.
     */
    public static class CompositeKey {
        private final Object[] components;

        private CompositeKey(Object[] components) {
            this.components = components;
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (Object component : components) {
                result = 31 * result + (component != null ? component.hashCode() : 0);
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            CompositeKey other = (CompositeKey) obj;
            if (components.length != other.components.length) return false;

            for (int i = 0; i < components.length; i++) {
                if (!objectEquals(components[i], other.components[i])) {
                    return false;
                }
            }

            return true;
        }

        private boolean objectEquals(Object a, Object b) {
            return (a == b) || (a != null && a.equals(b));
        }
    }
}
