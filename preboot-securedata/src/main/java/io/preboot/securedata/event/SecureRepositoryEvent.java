package io.preboot.securedata.event;

import io.preboot.eventbus.GenericEvent;
import java.time.LocalDateTime;

public sealed interface SecureRepositoryEvent<T> extends GenericEvent<T> {
    T getEntity();

    LocalDateTime getTimestamp();

    @Override
    default T getTypeParameter() {
        return getEntity();
    }

    record BeforeCreateEvent<T>(T entity, LocalDateTime timestamp) implements SecureRepositoryEvent<T> {
        public BeforeCreateEvent(T entity) {
            this(entity, LocalDateTime.now());
        }

        @Override
        public T getEntity() {
            return entity;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    record AfterCreateEvent<T>(T entity, LocalDateTime timestamp) implements SecureRepositoryEvent<T> {
        public AfterCreateEvent(T entity) {
            this(entity, LocalDateTime.now());
        }

        @Override
        public T getEntity() {
            return entity;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    record BeforeUpdateEvent<T>(T entity, LocalDateTime timestamp) implements SecureRepositoryEvent<T> {
        public BeforeUpdateEvent(T entity) {
            this(entity, LocalDateTime.now());
        }

        @Override
        public T getEntity() {
            return entity;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    record AfterUpdateEvent<T>(T entity, LocalDateTime timestamp) implements SecureRepositoryEvent<T> {
        public AfterUpdateEvent(T entity) {
            this(entity, LocalDateTime.now());
        }

        @Override
        public T getEntity() {
            return entity;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    record BeforeDeleteEvent<T>(T entity, LocalDateTime timestamp) implements SecureRepositoryEvent<T> {
        public BeforeDeleteEvent(T entity) {
            this(entity, LocalDateTime.now());
        }

        @Override
        public T getEntity() {
            return entity;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    record AfterDeleteEvent<T>(T entity, LocalDateTime timestamp) implements SecureRepositoryEvent<T> {
        public AfterDeleteEvent(T entity) {
            this(entity, LocalDateTime.now());
        }

        @Override
        public T getEntity() {
            return entity;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
