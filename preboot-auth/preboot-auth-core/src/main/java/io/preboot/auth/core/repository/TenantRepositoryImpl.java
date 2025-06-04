package io.preboot.auth.core.repository;

import io.preboot.auth.core.model.Tenant;
import io.preboot.query.FilterableFragmentContext;
import io.preboot.query.FilterableFragmentImpl;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepositoryImpl extends FilterableFragmentImpl<Tenant, Long> {

    protected TenantRepositoryImpl(final FilterableFragmentContext context) {
        super(context, Tenant.class);
    }
}
