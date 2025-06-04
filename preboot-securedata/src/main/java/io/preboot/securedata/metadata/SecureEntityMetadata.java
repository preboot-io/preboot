package io.preboot.securedata.metadata;

import io.preboot.securedata.annotation.*;
import io.preboot.securedata.exception.SecureDataException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.util.ReflectionUtils;

@Getter
public class SecureEntityMetadata<T> {
    private final Class<T> entityType;
    private final boolean enabled;
    private final Field tenantField;
    private final Field createdByField;
    private final Field createdAtField;
    private final Field modifiedByField;
    private final Field modifiedAtField;
    private final AccessRule[] readRules;
    private final AccessRule[] writeRules;

    public SecureEntityMetadata(Class<T> entityType) {
        this.entityType = entityType;

        DisableSecureEntity disableSecureEntity = entityType.getAnnotation(DisableSecureEntity.class);
        this.enabled = disableSecureEntity == null;

        this.tenantField = findAnnotatedField(entityType, Tenant.class);
        this.createdByField = findAnnotatedField(entityType, CreatedBy.class);
        this.createdAtField = findAnnotatedField(entityType, CreatedAt.class);
        this.modifiedByField = findAnnotatedField(entityType, ModifiedBy.class);
        this.modifiedAtField = findAnnotatedField(entityType, ModifiedAt.class);

        SecureAccess secureAccess = entityType.getAnnotation(SecureAccess.class);
        this.readRules = secureAccess != null ? secureAccess.read() : new AccessRule[0];
        this.writeRules = secureAccess != null ? secureAccess.write() : new AccessRule[0];
    }

    private Field findAnnotatedField(Class<?> type, Class<? extends java.lang.annotation.Annotation> annotation) {
        Field field = ReflectionUtils.findField(type, f -> f.isAnnotationPresent(annotation));
        if (field != null) {
            validateFieldType(field, annotation);
        }
        return field;
    }

    private void validateFieldType(Field field, Class<? extends java.lang.annotation.Annotation> annotation) {
        if ((annotation == CreatedAt.class || annotation == ModifiedAt.class)
                && !field.getType().equals(Instant.class)) {
            throw new SecureDataException(String.format(
                    "Field '%s' with @%s annotation must be of type java.time.Instant",
                    field.getName(), annotation.getSimpleName()));
        }

        if ((annotation == CreatedBy.class || annotation == ModifiedBy.class)
                && !field.getType().equals(UUID.class)) {
            throw new SecureDataException(String.format(
                    "Field '%s' with @%s annotation must be of type java.util.UUID",
                    field.getName(), annotation.getSimpleName()));
        }
    }

    public boolean hasTenantField() {
        return tenantField != null;
    }

    public boolean requiresTenant() {
        return hasTenantField() && tenantField.getAnnotation(Tenant.class).required();
    }

    public boolean hasCreatedByField() {
        return createdByField != null;
    }

    public boolean hasCreatedAtField() {
        return createdAtField != null;
    }

    public boolean hasModifiedByField() {
        return modifiedByField != null;
    }

    public boolean hasModifiedAtField() {
        return modifiedAtField != null;
    }
}
