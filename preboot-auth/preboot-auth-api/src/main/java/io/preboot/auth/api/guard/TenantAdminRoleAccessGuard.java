package io.preboot.auth.api.guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Security annotation that ensures only users with 'ADMIN' role can access the annotated method or class for their own
 * tenant.
 */
@Inherited
@PreAuthorize("hasRole('ADMIN')")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TenantAdminRoleAccessGuard {}
