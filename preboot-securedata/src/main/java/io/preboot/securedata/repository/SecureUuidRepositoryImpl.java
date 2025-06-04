package io.preboot.securedata.repository;

import io.preboot.eventbus.EventPublisher;
import io.preboot.query.FilterCriteria;
import io.preboot.query.FilterableUuidFragmentImpl;
import io.preboot.query.HasUuid;
import io.preboot.query.SearchParams;
import io.preboot.securedata.annotation.AccessRule;
import io.preboot.securedata.context.SecurityContext;
import io.preboot.securedata.context.SecurityContextProvider;
import io.preboot.securedata.event.SecureRepositoryEvent;
import io.preboot.securedata.exception.SecureDataException;
import io.preboot.securedata.metadata.SecureEntityMetadata;
import io.preboot.securedata.metadata.SecureEntityMetadataCache;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.util.ReflectionUtils;

public abstract class SecureUuidRepositoryImpl<T extends HasUuid, ID> extends FilterableUuidFragmentImpl<T, ID> {
    private final SecurityContextProvider securityContextProvider;
    private final SecureEntityMetadataCache metadataCache;
    private final EventPublisher eventPublisher;

    protected SecureUuidRepositoryImpl(SecureRepositoryContext context, Class<T> entityType) {
        super(context.getFilterableContext(), entityType);
        this.securityContextProvider = context.getSecurityContextProvider();
        this.metadataCache = context.getMetadataCache();
        this.eventPublisher = context.getEventPublisher();
    }

    // Override FilterableRepository methods to add security
    @Override
    public Page<T> findAll(SearchParams params) {
        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (!metadata.isEnabled()) {
            return super.findAll(params);
        }

        validateReadAccess(metadata);
        SearchParams secureParams = addSecurityConstraints(params, metadata);
        return super.findAll(secureParams);
    }

    @Override
    public Optional<T> findOne(SearchParams params) {
        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (!metadata.isEnabled()) {
            return super.findOne(params);
        }

        validateReadAccess(metadata);
        SearchParams secureParams = addSecurityConstraints(params, metadata);
        return super.findOne(secureParams);
    }

    @Override
    public long count(SearchParams params) {
        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (!metadata.isEnabled()) {
            return super.count(params);
        }

        validateReadAccess(metadata);
        SearchParams secureParams = addSecurityConstraints(params, metadata);
        return super.count(secureParams);
    }

    // Implement CrudRepository methods with security
    @Override
    public <S extends T> S save(S entity) {
        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (metadata.isEnabled() && metadata.hasTenantField()) {
            populateTenantId(entity, metadata);
        }
        if (metadata.isEnabled()) {
            validateWriteAccess(metadata);
        }

        boolean isNew = isNewEntity(entity);
        populateAuditFields(entity, metadata, isNew);

        if (isNew) {
            entity.setUuid(UUID.randomUUID());
            eventPublisher.publish(new SecureRepositoryEvent.BeforeCreateEvent<>(entity));
        } else {
            eventPublisher.publish(new SecureRepositoryEvent.BeforeUpdateEvent<>(entity));
        }

        S savedEntity = super.save(entity);

        if (isNew) {
            eventPublisher.publish(new SecureRepositoryEvent.AfterCreateEvent<>(savedEntity));
        } else {
            eventPublisher.publish(new SecureRepositoryEvent.AfterUpdateEvent<>(savedEntity));
        }

        return savedEntity;
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (metadata.isEnabled()) {
            validateWriteAccess(metadata);
        }

        Map<S, Boolean> entityStates =
                new IdentityHashMap<>(); // IdentityHashMap is a specialized Map implementation that uses reference
        // equality (==) instead of equals() when comparing keys.

        for (S entity : entities) {
            if (metadata.isEnabled() && metadata.hasTenantField()) {
                populateTenantId(entity, metadata);
            }

            boolean isNew = isNewEntity(entity);
            populateAuditFields(entity, metadata, isNew);

            entityStates.put(entity, isNew);

            if (isNew) {
                entity.setUuid(UUID.randomUUID());
                eventPublisher.publish(new SecureRepositoryEvent.BeforeCreateEvent<>(entity));
            } else {
                eventPublisher.publish(new SecureRepositoryEvent.BeforeUpdateEvent<>(entity));
            }
        }

        Iterable<S> savedEntities = super.saveAll(entities);

        for (S savedEntity : savedEntities) {
            boolean wasNew = entityStates.get(savedEntity);

            // Publish after event
            if (wasNew) {
                eventPublisher.publish(new SecureRepositoryEvent.AfterCreateEvent<>(savedEntity));
            } else {
                eventPublisher.publish(new SecureRepositoryEvent.AfterUpdateEvent<>(savedEntity));
            }
        }

        return savedEntities;
    }

    @Override
    public void delete(T entity) {
        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (metadata.isEnabled() && !validateTenantAccess(entity, metadata)) {
            throw new SecureDataException("Access denied");
        }
        if (metadata.isEnabled()) {
            validateWriteAccess(metadata);
        }
        eventPublisher.publish(new SecureRepositoryEvent.BeforeDeleteEvent<>(entity));
        super.delete(entity);
        eventPublisher.publish(new SecureRepositoryEvent.AfterDeleteEvent<>(entity));
    }

    @Override
    public Optional<T> findById(ID id) {
        Optional<T> result = super.findById(id);
        if (result.isEmpty()) {
            return result;
        }

        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (!metadata.isEnabled()) {
            return result;
        }
        validateReadAccess(metadata);

        T entity = result.get();
        if (validateTenantAccess(entity, metadata)) {
            return result;
        }

        return Optional.empty();
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<T> findAll() {
        // Convert to SearchParams to reuse security logic
        return findAll(SearchParams.empty().setUnpaged(true)).getContent();
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        // This needs tenant filtering
        Iterable<T> results = super.findAllById(ids);
        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (!metadata.isEnabled()) {
            return results;
        }
        validateReadAccess(metadata);

        List<T> secureResults = new ArrayList<>();
        results.forEach(entity -> {
            if (validateTenantAccess(entity, metadata)) {
                secureResults.add(entity);
            }
        });
        return secureResults;
    }

    @Override
    public Optional<T> findByUuid(UUID uuid) {
        Optional<T> result = super.findByUuid(uuid);
        if (result.isEmpty()) {
            return result;
        }

        SecureEntityMetadata<T> metadata = metadataCache.get(getEntityType());
        if (!metadata.isEnabled()) {
            return result;
        }
        validateReadAccess(metadata);

        T entity = result.get();
        if (validateTenantAccess(entity, metadata)) {
            return result;
        }

        return Optional.empty();
    }

    @Override
    public boolean existsByUuid(UUID uuid) {
        return findByUuid(uuid).isPresent();
    }

    @Override
    public void deleteByUuid(UUID uuid) {
        findByUuid(uuid).ifPresent(this::delete);
    }

    @Override
    public long count() {
        return count(SearchParams.empty());
    }

    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        ids.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        throw new SecureDataException("Bulk deleteAll() is not supported for secure repositories");
    }

    private boolean validateTenantAccess(T entity, SecureEntityMetadata<T> metadata) {
        if (!metadata.hasTenantField()) {
            return true;
        }

        try {
            Field tenantField = metadata.getTenantField();
            tenantField.setAccessible(true);
            UUID entityTenantId = (UUID) tenantField.get(entity);
            tenantField.setAccessible(false);

            UUID currentTenantId = securityContextProvider.getCurrentContext().getTenantId();
            return Objects.equals(entityTenantId, currentTenantId);
        } catch (IllegalAccessException e) {
            throw new SecureDataException("Failed to validate tenant access", e);
        }
    }

    private void populateTenantId(T entity, SecureEntityMetadata<T> metadata) {
        try {
            Field tenantField = metadata.getTenantField();
            tenantField.setAccessible(true);

            if (tenantField.get(entity) == null) {
                tenantField.set(
                        entity, securityContextProvider.getCurrentContext().getTenantId());
            } else {
                if (!validateTenantAccess(entity, metadata)) {
                    throw new SecureDataException("Access denied");
                }
            }

            tenantField.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new SecureDataException("Failed to set tenant ID", e);
        }
    }

    private SearchParams addSecurityConstraints(SearchParams params, SecureEntityMetadata<T> metadata) {
        SecurityContext securityContext = securityContextProvider.getCurrentContext();
        List<FilterCriteria> filters = new ArrayList<>();

        // Add existing filters
        if (params.getFilters() != null) {
            filters.addAll(params.getFilters());
        }

        // Add tenant filter if required
        if (metadata.requiresTenant()) {
            String tenantFieldName = metadata.getTenantField().getName();
            filters.add(FilterCriteria.eq(tenantFieldName, securityContext.getTenantId()));
        }

        // Create new params with security constraints
        return SearchParams.builder()
                .filters(filters)
                .page(params.getPage())
                .size(params.getSize())
                .sortField(params.getSortField())
                .sortDirection(params.getSortDirection())
                .build();
    }

    private <S extends T> boolean isNewEntity(S entity) {
        try {
            // Try to get the ID field using reflection
            Field idField = ReflectionUtils.findField(
                    entity.getClass(),
                    field -> field.isAnnotationPresent(org.springframework.data.annotation.Id.class));

            if (idField != null) {
                idField.setAccessible(true);
                Object id = idField.get(entity);
                idField.setAccessible(false);
                return id == null;
            }
            return true; // If we can't find an ID field, assume it's new
        } catch (Exception e) {
            return true; // If we can't determine, assume it's new
        }
    }

    private void validateReadAccess(SecureEntityMetadata<T> metadata) {
        if (!validateAccessRules(metadata.getReadRules())) {
            throw new SecureDataException("Read access denied - insufficient privileges");
        }
    }

    private void validateWriteAccess(SecureEntityMetadata<T> metadata) {
        if (!validateAccessRules(metadata.getWriteRules())) {
            throw new SecureDataException("Write access denied - insufficient privileges");
        }
    }

    private boolean validateAccessRules(AccessRule[] rules) {
        if (rules.length == 0) {
            return true; // No rules means unrestricted access
        }

        SecurityContext context = securityContextProvider.getCurrentContext();
        Set<String> userRoles = context.getRoles();

        // User must have at least one role from each AccessRule
        for (AccessRule rule : rules) {
            boolean hasRequiredRole = false;
            for (String requiredRole : rule.roles()) {
                if (userRoles.contains(requiredRole)) {
                    hasRequiredRole = true;
                    break;
                }
            }
            if (!hasRequiredRole) {
                return false;
            }
        }
        return true;
    }

    private <S extends T> void populateAuditFields(S entity, SecureEntityMetadata<T> metadata, boolean isNew) {
        SecurityContext context = securityContextProvider.getCurrentContext();
        try {
            if (isNew) {
                // Set created by/at fields only for new entities
                if (metadata.hasCreatedByField()) {
                    Field field = metadata.getCreatedByField();
                    field.setAccessible(true);
                    field.set(entity, context.getUserId());
                    field.setAccessible(false);
                }

                if (metadata.hasCreatedAtField()) {
                    Field field = metadata.getCreatedAtField();
                    field.setAccessible(true);
                    field.set(entity, Instant.now());
                    field.setAccessible(false);
                }
            } else {
                if (metadata.hasModifiedByField()) {
                    Field field = metadata.getModifiedByField();
                    field.setAccessible(true);
                    field.set(entity, context.getUserId());
                    field.setAccessible(false);
                }

                if (metadata.hasModifiedAtField()) {
                    Field field = metadata.getModifiedAtField();
                    field.setAccessible(true);
                    field.set(entity, Instant.now());
                    field.setAccessible(false);
                }
            }
        } catch (IllegalAccessException e) {
            throw new SecureDataException("Failed to set audit fields", e);
        }
    }
}
